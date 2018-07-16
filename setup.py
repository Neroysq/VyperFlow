# -*- coding: utf-8 -*-

from setuptools import setup, find_packages


with open('README.md') as f:
    readme = f.read()

setup(
    name='vyperflow',
    # *IMPORTANT*: Don't manually change the version here. Use the 'bumpversion' utility.
    version='0.1.0-alpha',
    description='VyperFlow Programming Language for Ethereum',
    long_description=readme,
    author='Siqiu Yao, Haobin Ni',
    author_email='',
    packages=find_packages(),
    python_requires='>=3.6',
    install_requires=['py-evm==0.2.0a18'],
    setup_requires=['pytest-runner'],
    tests_require=[
       'pytest',
       'pytest-cov',
       'eth-tester==0.1.0b26',
       'py-evm==0.2.0a18',
    ],
    scripts=[
       'bin/vif',
    ]
)
