# 🐘🏎️ PostgreSQL Concurrency Control Guide 
## Repository that demonstrates basic concurrency concepts in PostgreSQL with Java examples

#### 📂 `anomalies/`
- [NonRepeatableReadExample](cuncurency/src/main/java/com/rymar/concurency/anomaly/NonRepeatableRead.java)
- [Phantom read](cuncurency/src/main/java/com/rymar/concurency/anomaly/PhantomRead.java)
- [Serialization anomaly](cuncurency/src/main/java/com/rymar/concurency/anomaly/SerializableAnomaly.java)

#### 📂 `internal/`
- [MVCC](cuncurency/src/main/java/com/rymar/concurency/internal/MVCCDemo.java)
- [VACUUM](cuncurency/src/main/java/com/rymar/concurency/internal/VacuumDemo.java)
- [HOT](cuncurency/src/main/java/com/rymar/concurency/internal/HOTUpdate.java)