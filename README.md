Οδηγίες για compile και εκτέλεση του project μέσω Command Prompt (CMD).

---

## Setup

Άνοιξε το Command Prompt (CMD) και πήγαινε στον βασικό φάκελο του project:

```bash
cd path\to\Katanemimena-Systimata-2026
```

---

## Compile

Τρέξε τις παρακάτω εντολές:

```bash
dir /s /B *.java > sources.txt
javac -cp "Lib*;Shared\src" @sources.txt
```

---

## Run

### 🔹 Random Generator Server (SRG)

```bash
java -cp "Lib*;Shared\src;Random-Generator-Server\src" com.aueb.srg.RandomGeneratorServer
```

---

### 🔹 Reducer Server

```bash
java -cp "Lib*;Shared\src;Reducer\src" com.aueb.reducer.ReducerServer
```

---

### 🔹 Master Server

```bash
java -cp "Lib*;Shared\src;Master-Server\src" com.aueb.master.MasterServer
```

---

### 🔹 Worker Nodes

Τρέξε έναν worker για κάθε port (π.χ. 5001, 5002, 5003):

```bash
java -cp "Lib*;Shared\src;Worker-Node\src" com.aueb.worker.WorkerServer 5001
```

```bash
java -cp "Lib*;Shared\src;Worker-Node\src" com.aueb.worker.WorkerServer 5002
```

```bash
java -cp "Lib*;Shared\src;Worker-Node\src" com.aueb.worker.WorkerServer 5003
```

---

### 🔹 Manager Console

```bash
java -cp "Lib\*;Shared\src;Manager-Console\src" com.aueb.manager.ManagerClient
```

---