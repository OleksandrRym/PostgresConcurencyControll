# 🐘🏎️ PostgreSQL Concurrency Control Guide 
## Repository that demonstrates basic concurrency concepts in PostgreSQL with Java examples

### 📂 `concurrency/`
#### 📂 `anomalies/`
Contains examples of concurrency anomalies that can occur in PostgreSQL:
- [NonRepeatableReadExample](concurency/anomaly/NonRepeatableRead.java)
- [Phantom read](concurency/anomaly/PhantomRead.java)
- [Serialization anomaly](concurency/anomaly/SerializableAnomaly.java)

#### 📂 `locking/`
Contains examples of locking mechanisms in PostgreSQL:
- Row-level locks
- Table-level locks

