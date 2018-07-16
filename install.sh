virtualenv -p python3 --no-site-packages ./vif-venv
source ./vif-venv/bin/activate
make
cd Sherrloc
ant
