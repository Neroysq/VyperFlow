package sherrloc.diagnostic;

import java.util.List;
import java.util.Set;

import sherrloc.constraint.ast.Axiom;
import sherrloc.constraint.ast.Constraint;
import sherrloc.constraint.ast.Hypothesis;

/**
 * Result of parser
 */
public class DiagnosisInput {
	private Hypothesis env;
	private Set<Constraint> constraints;
	private List<Axiom> axioms;

	/**
	 * @param env
	 *            Global assumptions
	 * @param cons
	 *            A set of constraints
	 */
	public DiagnosisInput(Hypothesis env, Set<Constraint> cons, List<Axiom> axioms) {
		this.env = env;
		constraints = cons;
		this.axioms = axioms;
	}

	/**
	 * @return Global assumptions
	 */
	public Hypothesis getEnv() {
		return env;
	}

	/**
	 * @return A set of constraints in the input file
	 */
	public Set<Constraint> getConstraints() {
		return constraints;
	}
	
	/**
	 * @return A list (global) axioms in the input file
	 */
	public List<Axiom> getAxioms() {
		return axioms;
	}
}
