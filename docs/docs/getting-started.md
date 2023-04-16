#Getting started
To see ceris in action start ceris in demo mode, which creates a ``source`` connector 
generating demo data and two `sink` connectors writing the data to a csv file and log output.

1. Download the latest [release](https://github.com/eblaas/ceris/releases)
2. Run jar
    ```
    java -jar ceris-x.x.x.jar -Dceris.demo
    ```
3. Or run with docker
   ```
   docker run --name ceris -p 4567:4567 -e CERIS_DEMO=true eblaas/ceris
   ```
4. Login ceris UI [http://localhost:4567](http://localhost:4567) username=admin, password=admin