# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|

  # Kehitystyön tietokantapalvein
  config.vm.define "pairingsdb" do |db|
    db.vm.box = "hashicorp/precise64"

    db.vm.synced_folder "env", "/env"
    db.vm.provision "shell", inline: "cd /env && ./postgresql.sh"

    # local port 2345 -> vm port 5432
    db.vm.network "forwarded_port", host: 5432, guest: 5432
    db.vm.network "private_network", ip: "192.168.50.51"
  end
end
