#!/bin/bash
cd /home/ubuntu
sudo chown -R root:root /home/ubuntu/
source /etc/profile
sudo chmod +x /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar
sudo kill -9 $(lsof -t -i:8080)
nohup java -jar /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar > /home/ubuntu/output.txt &