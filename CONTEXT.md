# Finance

The MoneyMind backend bounded context: it ingests bank transaction files, classifies each transaction into a category, persists them, and serves search, aggregation, and review over them. Hexagonal architecture — domain owns the ports, infrastructure provides the adapters.

## Language

### Core records

**FinancialRecord**:
A single persisted bank transaction — id, bank, date, description, amount, balance, category.
_Avoid_: transaction (ambiguous with the classifier's own `Transaction` type), entry, row.

**ClassifiedFinancialRecord**:
A `FinancialRecord` paired with a predicted category and a confidence score, produced by classification.
_Avoid_: prediction, result.

**AggregatedResult**:
A grouped, summed row: an optional `period`, an optional `groupKey` (column value), and a `total`.
_Avoid_: summary (reserved for the guest-review `Summary`), group, bucket.

### Search & pagination

**TransactionSearchQuery**:
The parameter object carrying every search input — filters (id, category, bank, date range, excludeCategories), `aggregateByPeriod` / `aggregateByColumn`, sort, limit, cursor.
_Avoid_: filter, criteria, params, request.

**TransactionQuery**:
The deep module that builds and executes every query shape from a `TransactionSearchQuery`, exposing `search` (→ `PagedResult<FinancialRecord>`) and `searchAggregated` (→ `PagedResult<AggregatedResult>`). Owns filters, sort, period truncation, and pagination.
_Avoid_: query builder, strategy, helper, repository (the repository is a separate port it sits behind).

**SearchResult**:
A sealed type expressing the outcome of `SearchTransactions.execute()`: either `Records(PagedResult<FinancialRecord>)` (regular transactions) or `Aggregated(PagedResult<AggregatedResult>)` (grouped rows). The thing the web layer pattern-matches on to choose a response envelope.
_Avoid_: response, either, union.

**PagedResult**:
A page of items plus the effective `limit` and the next `cursor` (null when no further page).
_Avoid_: page (reserved for the HTTP `Page` envelope), slice.

**Cursor**:
The opaque pagination token. One module owns its format and its encode/decode for every variant (single id; period; period + column).
_Avoid_: offset, token, page key, bookmark.

**AggregationPeriod**:
The time granularity of a period aggregation — `YEAR` or `MONTH` — and the single source mapping each to its SQL truncation and date format.
_Avoid_: bare "period" (that's the resulting label on an `AggregatedResult`), granularity, interval.

### Classification & categories

**TransactionClassifier**:
The domain port that classifies records into categories. Its single live method takes a list of `FinancialRecord` and returns `ClassifiedFinancialRecord`s.
_Avoid_: predictor, ML service.

**Category / Subcategory**:
A category tree: top-level Categories contain Subcategories. System categories are global (`user_id` null).
_Avoid_: tag, label, bucket.

**CategoryType**:
The role a category plays in a summary — `INCOME`, `EXPENSE`, or `EXCLUDED`.
_Avoid_: kind, classification.

### Guest review

**GuestImport**:
The stateless use case that parses an uploaded file in memory, classifies it, and produces a `GuestReview` — deliberately with no persistence and no `TransactionRepository`.
_Avoid_: trial import, preview.

**GuestReview / MonthlyReview**:
The result of a guest import: the detected bank plus one `MonthlyReview` per month (income, expense, savings, category slices).
_Avoid_: report, snapshot.

### Summary

**SummaryEngine / Summary**:
The pure module (and its result) computing income, expense, savings, and per-category/subcategory breakdowns from a list of summary transactions.
_Avoid_: aggregator, calculator, stats.

### Banks

**BankRegistry / TransactionsParser**:
The port that lists available banks and yields a `TransactionsParser` per bank; each parser turns a bank's CSV/XLS into `FinancialRecord`s.
_Avoid_: importer, reader, loader.
