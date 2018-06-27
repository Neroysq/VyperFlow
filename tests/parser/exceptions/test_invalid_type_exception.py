import pytest
from pytest import raises

from vyper import compiler
from vyper.exceptions import InvalidTypeException

fail_list = [
    """
x: bat
    """,
    """
x: 5
    """,
    """
x: int128[int]
    """,
    """
x: int128[-1]
    """,
    """
x: int128[3.5]
    """,
    """
x: {int128[5]: int128[7]}
    """,
    """
x: [bar, baz]
    """,
    """
x: [bar(int128), baz(baffle)]
    """,
    """
x: {bar: int128, decimal: int128}
    """,
    """
x: {bar: int128, 5: int128}
    """,
    """
def foo(x): pass
    """,
    """
b: {num: int128, address: address}
    """,
    """
b: {num: int128, address: address}
    """,
    """
b: int128[int128, decimal]
    """,
    """
b: int128[int128: address]
    """,
    """
x: int128[address[bool]]
@public
def foo() -> int128(wei / sec):
    pass
    """,
    """
@public
def foo() -> {cow: int128, dog: int128}:
    return {cow: 5, dog: 7}
    """,
    """
x: wei(wei)
    """,
    """
x: int128(address)
    """,
    """
x: int128(wei and sec)
    """,
    """
x: int128(2 ** 2)
    """,
    """
x: int128(wei ** -1)
    """,
    """
x: int128(wei >> 3)
    """,
    """
x: bytes <= wei
    """,
    """
x: string <= 33
    """,
    """
x: bytes[1:3]
    """,
    """
x: bytes[33.3]
    """,
]


@pytest.mark.parametrize('bad_code', fail_list)
def test_invalid_type_exception(bad_code):
    with raises(InvalidTypeException):
        compiler.compile(bad_code)
