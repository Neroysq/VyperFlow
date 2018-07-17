git submodule update --init
virtualenv -p python3.6 --no-site-packages ./vif-venv
source ./vif-venv/bin/activate
make
