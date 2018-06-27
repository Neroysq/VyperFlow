import pytest
from pytest import raises

from vyper import compiler
from vyper.exceptions import StructureException


fail_list = [
    """
@public
def unmatched_tupl_length() -> (bytes[8], int128, bytes[8]):
    return "test", 123
    """
]


@pytest.mark.parametrize('bad_code', fail_list)
def test_tuple_return_fail(bad_code):

    with raises(StructureException):
            compiler.compile(bad_code)
