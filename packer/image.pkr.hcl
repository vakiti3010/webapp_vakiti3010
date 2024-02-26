packer {
  required_plugins {
    googlecompute = {
      version = "~> 1.0"
      source  = "github.com/hashicorp/googlecompute"
    }
  }
}

variable "source_file" {
  type    = string
  default = ""
}

variable "tmp_service_file" {
  type    = string
  default = ""
}

source "googlecompute" "centos-stream-8" {
  project_id          = "vakiti-dev"
  zone                = "us-central1-a"
  image_name          = "my-app-image-{{timestamp}}"
  source_image_family = "centos-stream-8"
  ssh_username        = "centos"
}

build {
  sources = ["source.googlecompute.centos-stream-8"]

  provisioner "shell" {
    inline = [
      "sudo groupadd csye6225",                                   // Create the group first
      "sudo useradd -m -g csye6225 -s /usr/sbin/nologin csye6225" // Create the user
    ]
  }
  provisioner "shell" {
    inline = [
      "sudo mkdir -p /home/csye6225",
      "sudo chown centos:centos /home/csye6225"
    ]
  }

  provisioner "file" {
    source      = "${var.sourcefile}"
    destination = "/home/csye6225/your-app.jar"
  }

  provisioner "file" {
    source      = "${var.tmp_service_file}"
    destination = "/tmp/your-app.service"
  }

  provisioner "shell" {
    script = "packer/provision.sh"
  }
}

