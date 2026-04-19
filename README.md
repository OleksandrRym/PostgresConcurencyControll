# 🐘🏎️ PostgreSQL Concurrency Control Guide 
## Repository that demonstrates basic concurrency concepts in PostgreSQL with Java examples

### 📂 `concurrency/`
#### 📂 `anomalies/`
Contains examples of concurrency anomalies that can occur in PostgreSQL:
- [NonRepeatableReadExample](.cuncurency/src/main/java/com/rymar/concurency/anomaly/NonRepeatableRead.java)
- [Phantom read](.cuncurency/src/main/java/com/rymar/concurency/anomaly/PhantomRead.java)
- [Serialization anomaly](.cuncurency/src/main/java/com/rymar/concurency/anomaly/SerializableAnomaly.java)

#### 📂 `locking/`
Contains examples of locking mechanisms in PostgreSQL:
- Row-level locks
- Table-level locks

