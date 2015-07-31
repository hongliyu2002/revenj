﻿using System;
using System.Collections.Concurrent;
using System.ComponentModel.Composition;
using System.Diagnostics.Contracts;
using System.Net;
using System.Runtime.Serialization;
using System.Security;
using Revenj.Common;
using Revenj.DomainPatterns;
using Revenj.Extensibility;
using Revenj.Processing;
using Revenj.Security;
using Revenj.Serialization;
using Revenj.Utility;
using System.Security.Principal;

namespace Revenj.Plugins.Server.Commands
{
	[Export(typeof(IServerCommand))]
	[ExportMetadata(Metadata.ClassType, typeof(QueueEvent))]
	public class QueueEvent : IReadOnlyServerCommand
	{
		private static ConcurrentDictionary<Type, IQueueCommand> Cache = new ConcurrentDictionary<Type, IQueueCommand>(1, 127);

		private readonly IDomainModel DomainModel;
		private readonly IPermissionManager Permissions;

		public QueueEvent(
			IDomainModel domainModel,
			IPermissionManager permissions)
		{
			Contract.Requires(domainModel != null);
			Contract.Requires(permissions != null);

			this.DomainModel = domainModel;
			this.Permissions = permissions;
		}

		[DataContract(Namespace = "")]
		public class Argument<TFormat>
		{
			[DataMember]
			public string Name;
			[DataMember]
			public TFormat Data;
		}

		private static TFormat CreateExampleArgument<TFormat>(ISerialization<TFormat> serializer)
		{
			return serializer.Serialize(new Argument<TFormat> { Name = "Module.Event" });
		}

		public ICommandResult<TOutput> Execute<TInput, TOutput>(
			IServiceProvider locator,
			ISerialization<TInput> input,
			ISerialization<TOutput> output,
			IPrincipal principal,
			TInput data)
		{
			var either = CommandResult<TOutput>.Check<Argument<TInput>, TInput>(input, output, data, CreateExampleArgument);
			if (either.Error != null)
				return either.Error;
			var argument = either.Argument;

			var eventType = DomainModel.Find(argument.Name);
			if (eventType == null)
			{
				return
					CommandResult<TOutput>.Fail(
						"Couldn't find event type {0}.".With(argument.Name),
						@"Example argument: 
" + CommandResult<TOutput>.ConvertToString(CreateExampleArgument(output)));
			}
			if (!typeof(IDomainEvent).IsAssignableFrom(eventType))
			{
				return CommandResult<TOutput>.Fail(@"Specified type ({0}) is not a domain event. 
Please check your arguments.".With(argument.Name), null);
			}
			if (!Permissions.CanAccess(eventType.FullName, principal))
			{
				return
					CommandResult<TOutput>.Return(
						HttpStatusCode.Forbidden,
						default(TOutput),
						"You don't have permission to access: {0}.",
						argument.Name);
			}
			try
			{
				IQueueCommand command;
				if (!Cache.TryGetValue(eventType, out command))
				{
					var commandType = typeof(QueueEventCommand<>).MakeGenericType(eventType);
					command = Activator.CreateInstance(commandType) as IQueueCommand;
					Cache.TryAdd(eventType, command);
				}
				command.Queue(input, locator, argument.Data);

				return CommandResult<TOutput>.Return(HttpStatusCode.Accepted, default(TOutput), "Event queued");
			}
			catch (ArgumentException ex)
			{
				return CommandResult<TOutput>.Fail(
					ex.Message,
					ex.GetDetailedExplanation() + @"
Example argument: 
" + CommandResult<TOutput>.ConvertToString(CreateExampleArgument(output)));
			}
		}

		private interface IQueueCommand
		{
			void Queue<TInput>(
				ISerialization<TInput> input,
				IServiceProvider locator,
				TInput data);
		}

		private class QueueEventCommand<TEvent> : IQueueCommand
			where TEvent : IDomainEvent
		{
			public void Queue<TInput>(
				ISerialization<TInput> input,
				IServiceProvider locator,
				TInput data)
			{
				TEvent domainEvent;
				try
				{
					domainEvent = data != null ? input.Deserialize<TInput, TEvent>(data, locator) : Activator.CreateInstance<TEvent>();
				}
				catch (Exception ex)
				{
					throw new ArgumentException("Error deserializing domain event.", ex);
				}
				var domainStore = locator.Resolve<IDomainEventStore>();
				try
				{
					domainStore.Queue(domainEvent);
				}
				catch (SecurityException) { throw; }
				catch (Exception ex)
				{
					throw new ArgumentException(
						ex.Message,
						data == null
							? new FrameworkException("Error while queuing event: {0}. Data not sent.".With(ex.Message), ex)
							: new FrameworkException(@"Error while queuing event: {0}. Sent data: 
{1}".With(ex.Message, input.Serialize(domainEvent)), ex));
				}
			}
		}
	}
}