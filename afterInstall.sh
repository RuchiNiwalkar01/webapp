#!/bin/bash
cd /home/ubuntu
sudo chown -R ubuntu:ubuntu /home/ubuntu/
source /etc/profile
kill -SIGKILL $$
sudo chmod +x /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar
#kill -9 $(ps -ef | grep webapp | grep -v grep | awk '{print $2}')
nohup java -jar /home/ubuntu/webapp-0.0.1-SNAPSHOT.jar > /home/ubuntu/output.txt &