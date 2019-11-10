#!/usr/bin/env bash

set -euo pipefail

PGPASSWORD=pairings psql -hlocalhost -Upairings pairings << EOF
INSERT INTO trader_user (id, username, uuid) values (0, 'testuser', '5ec35a0e-851a-4cf6-b126-e7f24cf5e371')
ON CONFLICT DO NOTHING;
INSERT INTO smf_members (id_member, member_name, passwd) values (0, 'testuser', '4282d5230736902ce84a4b94c802c69508c6369b')
ON CONFLICT DO NOTHING;
EOF

echo "Inserted user testuser with id 0, password password and uuid 5ec35a0e-851a-4cf6-b126-e7f24cf5e371"
