![VyperFlow](logo/VyperFlow.png)

---
VyperFlow is a next-generation secure smart contract language for Ethereum.

VyperFlow extends [Vyper](https://github.com/ethereum/vyper) with support for [information flow](https://www.cs.cornell.edu/jif/).

For [IC3 Bootcamp 18](http://www.initc3.org/events/2018-07-12-IC3-Ethereum-Crypto-Boot-Camp.html).

## VyperFlow is Awesome!
Please checkout [`awesome-vyperflow`](https://github.com/FTRobbin/awesome-vyperflow-bootcamp) for motivating VyperFlow examples written by our working group members at IC3 Bootcamp!

## Getting Started

### Prerequisites
[Python 3.6](https://www.python.org/getit/)

**Caution: The current version of Vyper is not compatible with Python3.7 see this** [**issue**](https://github.com/ethereum/vyper/issues/945)

[virtualenv](https://virtualenv.pypa.io/en/stable/installation/)

[Apache ant](https://ant.apache.org/manual/install.html)

#### For Mac OS Users
Ensure the following libraries are installed using *brew*.
```sh
brew install gmp leveldb
```

For `fatal error: openssl/aes.h: No such file or directory`, please reference [this](http://vyper.readthedocs.io/en/latest/installing-vyper.html#installation).

### Install VyperFlow

Get the latest version of VyperFlow by cloning this repository, and run the installation script.

```sh
git clone https://github.com/Neroysq/VyperFlow
cd vyperflow
./install.sh
```

It will build VyperFlow in a virtual Python environment.

If the build succeeds, you should be ready to go!

## Compiling a contract

**Remember to enter the virtual environment to use VyperFlow.** You can do this by entering:

```sh
source vif-venv/bin/activate
```

You can compile an example contract by running:

```sh
vyper examples/wallet.vif
```

If everything works correctly, you should see the compiled bytecode as output.

---
[Siqiu Yao](https://github.com/Neroysq), [Haobin Ni](https://github.com/FTRobbin), Cornell University, 2018
