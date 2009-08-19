'''
Created on Aug 17, 2009

@author: Sergei Romanenko
'''

from sll_language import *
from algebra import *

# Enhanced homeomorphic embedding:
# expressions are compared only if they belong
# to the same category (as defined by `aVarIsUnderAttack`).

def embeddedIn(e1, e2):
    return aVarIsUnderAttack(e1) == aVarIsUnderAttack(e2) and he(e1, e2)

# This is the "classic" homeomorphic embedding relation.

def he(e1, e2):
    return heByDiving(e1, e2) or heByCoupling(e1, e2)

def heByDiving(e1, e2) :
    if isinstance(e2, Var):
        return False
    elif isinstance(e2, Call):
        return any(map(lambda e: he(e1, e), e2.args))

def heByCoupling(e1, e2):
    if isinstance(e1, Var) and isinstance(e2, Var):
        return True
    elif e1.hasTheSameFunctorAs(e2):
        return all(map(he, e1.args, e2.args))

# We distinguish a specific category of expressions:
# the ones that generate contractions in the process tree.

def aVarIsUnderAttack(e):
    if isinstance(e, GCall):
        return aVarIsUnderAttack(e.args[0])
    elif isinstance(e, Var):
        return True
    else:
        return False
