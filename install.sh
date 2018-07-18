#!/bin/bash

git submodule update --init
virtualenv -p python3.6 --no-site-packages vif-venv
. vif-venv/bin/activate
make
