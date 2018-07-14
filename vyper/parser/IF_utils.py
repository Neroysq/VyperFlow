def new_cons(llb, rlb, pos) :
    return {"left":llb, "right" : rlb, "pos" : pos}

def pos_str(pos) :
# pos = (lineno, coloffset)
    return "L" + str(pos[0]) + "C" + str(pos[1])

def eval_label(exp, IFLs) :
    if type(exp) == str :
        ppls = exp.strip().split("_")
    else :
        raise StructureException("label must be a string")
    rnt = {"meet" : False, "principals":[]}
    for ppl in ppls :
        pname = principal_trans(ppl)
        if pname not in ppls :
            IFLs[pname] = {"pos" : None, "const" : True}
        rnt["principals"].append(ppl)
    return rnt, IFLs

def principal_trans(s) :
    return "@" + s

def to_sherrlocname(s) :
    if s[0] == "@" :
        return "P_" + s[1:]
    else :
        return s

def to_sherrlocexp(exp) :
    if type(exp) == str :
        return to_sherrlocname(exp)
    op = " ⊓ " if exp["meet"] else " ⊔ "
    return op.join(set(exp["principals"]))

