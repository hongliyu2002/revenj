package gen.model._DatabaseCommon.Factorytest;



import java.io.*;
import java.util.*;
import java.util.stream.*;
import org.revenj.postgres.*;
import org.revenj.postgres.converters.*;

public class CompositeConverter implements ObjectConverter<gen.model.test.Composite> {

	public CompositeConverter(List<ObjectConverter.ColumnInfo> allColumns) throws java.io.IOException {
		Optional<ObjectConverter.ColumnInfo> column;
		
			
		final java.util.List<ObjectConverter.ColumnInfo> columns =
				allColumns.stream().filter(it -> "test".equals(it.typeSchema) && "Composite_entity".equals(it.typeName))
				.collect(Collectors.toList());
		columnCount = columns.size();
		readers = new ObjectConverter.Reader[columnCount];
		for (int i = 0; i < readers.length; i++) {
			readers[i] = (instance, rdr, ctx) -> StringConverter.skip(rdr, ctx);
		}
			
		final java.util.List<ObjectConverter.ColumnInfo> columnsExtended =
				allColumns.stream().filter(it -> "test".equals(it.typeSchema) && "-ngs_Composite_type-".equals(it.typeName))
				.collect(Collectors.toList());
		columnCountExtended = columnsExtended.size();
		readersExtended = new ObjectConverter.Reader[columnCountExtended];
		for (int i = 0; i < readersExtended.length; i++) {
			readersExtended[i] = (instance, rdr, ctx) -> StringConverter.skip(rdr, ctx);
		}
			
		column = columns.stream().filter(it -> "id".equals(it.columnName)).findAny();
		if (!column.isPresent()) throw new java.io.IOException("Unable to find 'id' column in test Composite_entity. Check if DB is in sync");
		__index___id = (int)column.get().order - 1;
			
		column = columnsExtended.stream().filter(it -> "id".equals(it.columnName)).findAny();
		if (!column.isPresent()) throw new java.io.IOException("Unable to find 'id' column in test Composite. Check if DB is in sync");
		__index__extended_id = (int)column.get().order - 1;
			
		column = columns.stream().filter(it -> "simple".equals(it.columnName)).findAny();
		if (!column.isPresent()) throw new java.io.IOException("Unable to find 'simple' column in test Composite_entity. Check if DB is in sync");
		__index___simple = (int)column.get().order - 1;
			
		column = columnsExtended.stream().filter(it -> "simple".equals(it.columnName)).findAny();
		if (!column.isPresent()) throw new java.io.IOException("Unable to find 'simple' column in test Composite. Check if DB is in sync");
		__index__extended_simple = (int)column.get().order - 1;
	}

	public void configure(org.revenj.patterns.ServiceLocator locator) {
		
		__converter_simple = locator.resolve(gen.model._DatabaseCommon.Factorytest.SimpleConverter.class);
		
			
		gen.model.test.Composite.configureConverter(readers, __index___id, __converter_simple, __index___simple);
			
		gen.model.test.Composite.configureConverterExtended(readersExtended, __index__extended_id, __converter_simple, __index__extended_simple);
	}

	@Override
	public gen.model.test.Composite from(PostgresReader reader) throws java.io.IOException {
		return from(reader, 0);
	}

	private gen.model.test.Composite from(PostgresReader reader, int outerContext, int context, ObjectConverter.Reader<gen.model.test.Composite>[] readers) throws java.io.IOException {
		reader.read(outerContext);
		gen.model.test.Composite instance = new gen.model.test.Composite(reader, context, readers);
		reader.read(outerContext);
		return instance;
	}

	@Override
	public PostgresTuple to(gen.model.test.Composite instance) {
		if (instance == null) return null;
		PostgresTuple[] items = new PostgresTuple[columnCount];
		
		items[__index___id] = org.revenj.postgres.converters.UuidConverter.toTupleNullable(instance.getId());
		items[__index___simple] = __converter_simple.to(instance.getSimple());
		return RecordTuple.from(items);
	}

	public PostgresTuple toExtended(gen.model.test.Composite instance) {
		if (instance == null) return null;
		PostgresTuple[] items = new PostgresTuple[columnCount];
		
		items[__index__extended_id] = org.revenj.postgres.converters.UuidConverter.toTupleNullable(instance.getId());
		items[__index__extended_simple] = __converter_simple.toExtended(instance.getSimple());
		return RecordTuple.from(items);
	}

	
		
	private final int columnCount;
	private final ObjectConverter.Reader<gen.model.test.Composite>[] readers;
		
	public gen.model.test.Composite from(PostgresReader reader, int context) throws java.io.IOException {
		int cur = reader.read();
		if (cur == ',' || cur == ')') return null;
		gen.model.test.Composite instance = from(reader, context, context == 0 ? 1 : context << 1, readers);
		reader.read();
		return instance;
	}
		
	private final int columnCountExtended;
	private final ObjectConverter.Reader<gen.model.test.Composite>[] readersExtended;
		
	public gen.model.test.Composite fromExtended(PostgresReader reader, int context) throws java.io.IOException {
		int cur = reader.read();
		if (cur == ',' || cur == ')') return null;
		gen.model.test.Composite instance = from(reader, context, context == 0 ? 1 : context << 1, readersExtended);
		reader.read();
		return instance;
	}
		private final int __index___id;
		private final int __index__extended_id;
	private gen.model._DatabaseCommon.Factorytest.SimpleConverter __converter_simple;
		private final int __index___simple;
		private final int __index__extended_simple;
}
