source "googlecompute" "centos-stream-8" {
  project_id     = "csye6225-spring"
  zone           = "us-central1-a"
  image_name     = "my-app-image-{{timestamp}}"
  source_image_family = "centos-stream-8"
  ssh_username   = "centos"
}

build {
  sources = ["source.googlecompute.centos-stream-8"]

  provisioner "shell" {
    inline = [
      "sudo groupadd csye6225",  // Create the group first
      "sudo useradd -m -g csye6225 -s /usr/sbin/nologin csye6225"  // Create the user
    ]
  }
  provisioner "shell" {
    inline = [
      "sudo mkdir -p /home/csye6225",
      "sudo chown centos:centos /home/csye6225"
    ]
  }

  provisioner "file" {
    source      = "../target/webapp-0.0.1-SNAPSHOT.jar"
    destination = "/home/csye6225/your-app.jar"
  }

  provisioner "file" {
    source = "your-app.service" # Adjust the path here if needed
    destination = "/tmp/your-app.service" # Temporary location
  }

  provisioner "shell" {
    script = "provision.sh"
  }
}
