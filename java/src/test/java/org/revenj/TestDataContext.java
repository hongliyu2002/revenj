package org.revenj;

import gen.model.Boot;
import gen.model.Seq.Next;
import gen.model.binaries.Document;
import gen.model.binaries.ReadOnlyDocument;
import gen.model.binaries.WritableDocument;
import gen.model.calc.Info;
import gen.model.calc.Realm;
import gen.model.calc.Type;
import gen.model.issues.TimestampPk;
import gen.model.mixinReference.Author;
import gen.model.mixinReference.SpecificReport;
import gen.model.test.Clicked;
import gen.model.test.Composite;
import gen.model.test.FindMany;
import gen.model.test.Simple;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.revenj.extensibility.Container;
import org.revenj.patterns.*;
import ru.yandex.qatools.embed.service.PostgresEmbeddedService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

public class TestDataContext {

	private PostgresEmbeddedService postgres;
	private Container container;

	@Before
	public void initContainer() throws IOException {
		postgres = Setup.database();
		container = (Container) Boot.configure("jdbc:postgresql://localhost:5555/revenj");
	}

	@After
	public void closeContainer() throws Exception {
		container.close();
		postgres.stop();
	}

	@Test
	public void canCreateAggregate() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Composite co = new Composite();
		UUID id = UUID.randomUUID();
		co.setId(id);
		Simple so = new Simple();
		so.setNumber(5);
		so.setText("test me ' \\ \" now");
		co.setSimple(so);
		context.create(co);
		Optional<Composite> found = context.find(Composite.class, id.toString());
		Assert.assertTrue(found.isPresent());
		Composite co2 = found.get();
		Assert.assertEquals(co.getId(), co2.getId());
		Assert.assertEquals(co.getSimple().getNumber(), co2.getSimple().getNumber());
		Assert.assertEquals(co.getSimple().getText(), co2.getSimple().getText());
	}

	@Test
	public void canSubmitEvent() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Clicked cl = new Clicked().setBigint(Long.MAX_VALUE).setDate(LocalDate.now()).setNumber(BigDecimal.valueOf(11.22));
		context.submit(cl);
	}

	@Test
	public void canQuery() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		long total1 = context.query(Next.class).count();
		context.create(Arrays.asList(new Next(), new Next()));
		long total2 = context.query(Next.class).count();
		Assert.assertEquals(total1 + 2, total2);
	}

	@Test
	public void canSearch() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		long total1 = context.search(Next.class).size();
		context.create(Arrays.asList(new Next(), new Next()));
		long total2 = context.search(Next.class).size();
		Assert.assertEquals(total1 + 2, total2);
	}

	@Test
	public void canCount() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		long total1 = context.count(Next.class);
		context.create(Arrays.asList(new Next(), new Next()));
		long total2 = context.count(Next.class);
		Assert.assertEquals(total1 + 2, total2);
	}

	@Test
	public void unitOfWork() throws IOException {
		ServiceLocator locator = container;
		try (UnitOfWork uow = locator.resolve(UnitOfWork.class)) {
			Next next = new Next();
			uow.create(next);
			Optional<Next> find = uow.find(Next.class, next.getURI());
			Assert.assertEquals(next, find.get());
		}
	}

	@Test
	public void transactionsWithUnitOfWork() throws IOException {
		ServiceLocator locator = container;
		try (UnitOfWork uow1 = locator.resolve(UnitOfWork.class);
			UnitOfWork uow2 = locator.resolve(UnitOfWork.class)) {
			Next next = new Next();
			uow1.create(next);
			Optional<Next> find1 = uow1.find(Next.class, next.getURI());
			Optional<Next> find2 = uow2.find(Next.class, next.getURI());
			Assert.assertEquals(next, find1.get());
			Assert.assertFalse(find2.isPresent());
		}
	}

	@Test
	public void referenceIDUpdate() throws Exception {
		ServiceLocator locator = container;
		SpecificReport report = new SpecificReport();
		Author author = new Author();
		report.setAuthor(author);
		Assert.assertEquals(author.getURI(), report.getAuthorURI());
		Assert.assertEquals(author.getID(), report.getAuthorID());
		DataContext context = locator.resolve(DataContext.class);
		context.create(author);
		Assert.assertEquals(author.getURI(), report.getAuthorURI());
		Assert.assertEquals(author.getID(), report.getAuthorID());
		context.create(report);
	}

	@Test
	public void readOnlySqlConcept() throws Exception {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Document document = new Document().setName("test me");
		context.create(document);
		UUID id = document.getID();
		Optional<ReadOnlyDocument> found =
				context.query(ReadOnlyDocument.class)
						.filter(it -> it.getID().equals(id))
						.findAny();
		Assert.assertTrue(found.isPresent());
		Assert.assertEquals("test me", found.get().getName());
	}

	@Test
	public void writableSqlConcept() throws Exception {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Document document = new Document().setName("test me now");
		context.create(document);
		UUID id = document.getID();
		Optional<WritableDocument> found =
				context.query(WritableDocument.class)
						.filter(it -> it.getId().equals(id))
						.findAny();
		Assert.assertTrue(found.isPresent());
		WritableDocument wd = found.get();
		Assert.assertEquals("test me now", wd.getName());
		wd.setName("test me later");
		context.update(wd);
		Optional<Document> changed = context.find(Document.class, document.getURI());
		Assert.assertTrue(changed.isPresent());
		Assert.assertEquals("test me later", changed.get().getName());
	}

	@Test
	public void persistableCalculatedPrimaryKey() throws Exception {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Random rnd = new Random();
		Type t = new Type().setSuffix("ab" + rnd.nextInt(100000)).setDescription("desc");
		context.create(t);
		Info i = new Info().setCode("xx" + rnd.nextInt(1000000)).setName("abcdef" + rnd.nextInt(1000000));
		context.create(i);
		Realm r = new Realm().setInfo(i).setRefType(t);
		context.create(r);
		Optional<Realm> found = context.find(Realm.class, r.getURI());
		Assert.assertTrue(found.isPresent());
		Assert.assertTrue(r.deepEquals(found.get()));
	}

	@Test
	public void canPopulateReport() throws IOException {
		ServiceLocator locator = container;
		DataContext context = locator.resolve(DataContext.class);
		Composite co = new Composite();
		UUID id = UUID.randomUUID();
		co.setId(id);
		context.create(co);
		FindMany.Result result = context.populate(new FindMany(id, new HashSet<>(Collections.singletonList(id))));
		Assert.assertEquals(id, result.getFound().getId());
	}

	@Test
	public void canLoadHistory() throws Exception {
		ServiceLocator locator = container;
		DataContext db = locator.resolve(DataContext.class);
		OffsetDateTime odt = OffsetDateTime.now();
		TimestampPk pk = new TimestampPk().setTs(odt).setD(BigDecimal.ONE);
		db.create(pk);
		String uri = pk.getURI();
		Optional<TimestampPk> found = db.find(TimestampPk.class, uri);
		Optional<History<TimestampPk>> foundHistory = db.history(TimestampPk.class, uri);
		Assert.assertTrue(found.isPresent());
		Assert.assertTrue(foundHistory.isPresent());
		Assert.assertEquals(1, foundHistory.get().getSnapshots().size());
		Assert.assertEquals(uri, foundHistory.get().getURI());
		//Assert.assertEquals(found.get(), foundHistory.get().getSnapshots().get(0).getValue());
		Assert.assertTrue(found.get().getTs().isEqual(foundHistory.get().getSnapshots().get(0).getValue().getTs()));
	}
}
