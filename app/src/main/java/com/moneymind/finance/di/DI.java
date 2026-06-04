package com.moneymind.finance.di;

import com.moneymind.classifier.ports.Classifier;
import com.moneymind.finance.domain.banks.ListBanks;
import com.moneymind.finance.domain.categories.ListCategories;
import com.moneymind.finance.domain.guest.GuestImport;
import com.moneymind.finance.domain.summary.SummaryEngine;
import com.moneymind.finance.domain.ports.BankRegistry;
import com.moneymind.finance.domain.ports.CategoryRepository;
import com.moneymind.finance.domain.ports.TransactionClassifier;
import com.moneymind.finance.domain.ports.TransactionRepository;
import com.moneymind.finance.domain.ports.TransactionsParser;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.domain.transactions.GetTransaction;
import com.moneymind.finance.domain.transactions.ImportTransactions;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import com.moneymind.finance.infrastructure.file.TransactionsParserFactory;
import com.moneymind.finance.infrastructure.postgres.CategoryStore;
import com.moneymind.finance.infrastructure.postgres.TransactionStore;
import com.moneymind.finance.infrastructure.txClassifier.ClassificationEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

@ApplicationScoped
public class DI {

    Logger LOG = Logger.getLogger(DI.class);

    @Inject
    DSLContext dataSource;

    @ApplicationScoped
    @Produces
    TransactionRepository transactionRepository(final DSLContext dataSource) {
        return new TransactionStore(dataSource);
    }

    @ApplicationScoped
    @Produces
    CategoryRepository categoryRepository(final DSLContext dataSource) {
        return new CategoryStore(dataSource);
    }

    @ApplicationScoped
    @Produces
    ListCategories listCategories(final CategoryRepository categoryRepository) {
        return new ListCategories(categoryRepository);
    }

    @ApplicationScoped
    @Produces
    BankRegistry bankRegistry(@Any Instance<TransactionsParser> availableParsers) {
        return new TransactionsParserFactory(availableParsers);
    }

    @ApplicationScoped
    @Produces
    ImportTransactions importTransactions(final BankRegistry bankRegistry,
                                          final TransactionRepository transactionRepository,
                                          final TransactionClassifier transactionClassifier) {
        return new ImportTransactions(bankRegistry, transactionRepository, transactionClassifier);
    }

    @ApplicationScoped
    @Produces
    SearchTransactions searchTransactions(final TransactionRepository transactionRepository) {
        return new SearchTransactions(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    GetTransaction getTransaction(final TransactionRepository transactionRepository) {
        return new GetTransaction(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    UpdateTransactions updateTransactions(final TransactionRepository transactionRepository) {
        return new UpdateTransactions(transactionRepository);
    }

    @ApplicationScoped
    @Produces
    ListBanks listBanks(final BankRegistry bankRegistry) {
        return new ListBanks(bankRegistry);
    }

    @ApplicationScoped
    @Produces
    ClassifyTransactions classifyTransactions(final TransactionClassifier transactionClassifier,
                                              final TransactionRepository transactionRepository) {
        return new ClassifyTransactions(transactionClassifier, transactionRepository);
    }

    @ApplicationScoped
    @Produces
    SummaryEngine summaryEngine() {
        return new SummaryEngine();
    }

    @ApplicationScoped
    @Produces
    GuestImport guestImport(final BankRegistry bankRegistry,
                            final TransactionClassifier transactionClassifier,
                            final CategoryRepository categoryRepository,
                            final SummaryEngine summaryEngine) {
        return new GuestImport(bankRegistry, transactionClassifier, categoryRepository, summaryEngine);
    }

    @ApplicationScoped
    @Produces
    TransactionClassifier transactionClassifier(final Classifier classifier,
                                                final TransactionRepository transactionRepository) {
        return new ClassificationEngine(classifier, transactionRepository);
    }
}