Άνοιξε το CMD και πήγαινε στον κεντρικό φάκελο Katanemimena-Systimata-2026.

1. Compile

Τρέξε αυτές τις δύο εντολές στο CMD:

dir /s /B *.java > sources.txt
javac -cp "Lib\*;Shared\src" @sources.txt

1. Run
   
SRG: java -cp "Lib\*;Shared\src;Random-Generator-Server\src" com.aueb.srg.RandomGeneratorServer

Reducer: java -cp "Lib\*;Shared\src;Reducer\src" com.aueb.reducer.ReducerServer

Master: java -cp "Lib\*;Shared\src;Master-Server\src" com.aueb.master.MasterServer

port arg gia workers 5001, 5002, 5003
Worker Node: java -cp "Lib\*;Shared\src;Worker-Node\src" com.aueb.worker.WorkerServer "port"

Manager Console: java -cp "Lib\*;Shared\src;Manager-Console\src" com.aueb.manager.ManagerClient