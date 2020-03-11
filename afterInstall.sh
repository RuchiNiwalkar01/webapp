#!/bin/bash
cd /home/ubuntu
sudo chown -R ubuntu:ubuntu /home/ubuntu/
source /etc/profile
sudo chmod +x /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar
sudo kill -9 $(sudo lsof -i tcp:8080)
nohup java -jar /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar > /home/ubuntu/output.txt &