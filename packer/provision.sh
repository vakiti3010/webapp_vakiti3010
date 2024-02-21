sudo mkdir -p /opt/your-app
sudo cp /home/csye6225/your-app.jar /opt/your-app/
sudo chown -R csye6225:csye6225 /opt/your-app
sudo yum install -y java-17-openjdk

# Install and configure MariaDB
sudo dnf install mariadb-server -y
sudo systemctl start mariadb
sudo mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'saikumar123';"

# Create the 'CSYE6225' database
sudo mysql -u root -e "CREATE DATABASE CSYE6225;"

sudo cp /tmp/your-app.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable your-app.service
