####################
Compiling a Contract
####################
To compile a contract, use:
::
    vyper yourFileName.vy

You can also compile to other formats such as ABI using the below format:
::
    vyper -f ['abi', 'json', 'bytecode', 'bytecode_runtime', 'ir'] yourFileName.vy

.. note::
    Since .vy is not officially a language supported by any syntax highlighters or linters,
    it is recommended to name your Vyper file ending with `.vy` or optionally `.vy` in order to have Python syntax highlighting.

An `online compiler <https://vyper.online/>`_ is available as well, which lets you experiment with
the language without having to install Vyper. The online compiler allows you to compile to ``bytecode`` and/or ``LLL``.

.. note::
    While the vyper version of the online compiler is updated on a regular basis it might
    be a bit behind the latest version found in the master branch of the repository.
