#!/bin/bash
set -eu

# asennetaan postgre
echo "deb http://apt.postgresql.org/pub/repos/apt/ precise-pgdg main" >> /etc/apt/sources.list.d/pgdg.list

apt-get update && apt-get install -y postgresql-10

# alustetaan postgre
service postgresql initdb

# postgre kuuntelemaan ja ottamaan vastaan yhteyksia muualta kuin localhostista
PG_HBA=/etc/postgresql/10/main/pg_hba.conf

echo 'local   all             all                  peer' > "$PG_HBA"
echo 'host    all             all             all  md5' >> "$PG_HBA"

chown postgres:postgres "$PG_HBA"

echo "listen_addresses = '*'" >> /etc/postgresql/10/main/postgresql.conf

# kayntiin
service postgresql start
update-rc.d postgresql defaults

sudo -u postgres psql --file=db.sql
