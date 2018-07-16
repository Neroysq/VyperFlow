package sherrloc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import sherrloc.diagnostic.DiagnosticOptions.Mode;
import sherrloc.diagnostic.ErrorDiagnosis;

/**
 * Tests
 */
public class TestAll {
		
	/**
	 * Test if there is an error in the program analysis
	 * 
	 * @param filename
	 *            Constraint inputs
	 * @param expectederror
	 *            True if the input constrains error
	 */
	public void testErrorPaths (String filename, boolean expectederror) {
		try {
			ErrorDiagnosis ana = ErrorDiagnosis.getAnalysisInstance(filename, Mode.EXPR);
			assertEquals(filename, expectederror, ana.getUnsatPathNumber()>0);
		}
		catch (Exception e) {
			System.out.println(filename);
			e.printStackTrace();
		}
	}
	
	/**
	 * Test if the error diagnosis identifies the true wrong expression
	 * 
	 * @param filename
	 *            Constraint inputs
	 * @param loc
	 *            Location of the true mistake
	 */
	public void testExpression (String filename, String loc) {
		try {
			ErrorDiagnosis ana = ErrorDiagnosis.getAnalysisInstance(filename, Mode.EXPR);
			String result = ana.toConsoleString();
			assertTrue("Expected ("+loc+"), but got ("+result+")", result.contains(loc));
		}
		catch (Exception e) {
			System.out.println(filename);
			e.printStackTrace();
		}
	}
	
	/**
	 * Test if the error diagnosis identifies the true wrong constraints (downgrade location)
	 * 
	 * @param filename
	 *            Constraint inputs
	 * @param loc
	 *            Location of the true mistake
	 */
	public void testConstraint (String filename, String loc) {
		try {
			ErrorDiagnosis ana = ErrorDiagnosis.getAnalysisInstance(filename, Mode.CONS);
			String result = ana.toConsoleString();
			assertTrue("Expected ("+loc+"), but got ("+result+")", result.contains(loc));
		}
		catch (Exception e) {
			System.out.println(filename);
			e.printStackTrace();
		}
	}
	
	/**
	 * Test if the error diagnosis identifies the true missing hypothesis
	 * 
	 * @param filename
	 *            Constraint inputs
	 * @param expected
	 *            The true missing hypothesis
	 */
	public void testAssumptions (String filename, String expected) {
		try {
			ErrorDiagnosis ana = ErrorDiagnosis.getAnalysisInstance(filename, Mode.HYPO);
			String result = ana.toConsoleString();
			assertTrue("Expected ("+expected+"), but got ("+result+")", result.contains(expected));
		}
		catch (Exception e) {
			System.out.println(filename);
			e.printStackTrace();
		}
	}
	
	@Test
	public void regressionTests () {
		testErrorPaths("tests/jif/skolemcheck.con", true);
		testErrorPaths("tests/jif/DashedPaths.con", false);
		testAssumptions("tests/jif/cluster.con", "- a <= c;a <= d;b <= c;b <= d;\n");
		testAssumptions("tests/jif/induction.con", "- A <= B;\n");		
		testAssumptions("tests/jif/induction2.con", "- (P)->(P) <= C_caller_pc;\n");
		testAssumptions("tests/jif/join.con", "- (P)->(P) <= C_caller_pc;\n");
		testAssumptions("tests/jif/meet.con", "- a <= b;a <= c;\n");
		testAssumptions("tests/jif/varconstructor.con", "- INT <= BOOL;\n");
		testAssumptions("tests/jif/consofvar.con", "- CON2 (x2) <= INT;\n");
		testAssumptions("tests/jif/consofvar2.con", "- list (list (BOOL)) <= Ord;\n");
		testErrorPaths("tests/jif/consofvar3.con", false);
		testErrorPaths("tests/jif/consofvar4.con", false);
		testErrorPaths("tests/jif/consofvar5.con", true);
		testErrorPaths("tests/jif/consofvar6.con", true);
		testAssumptions("tests/jif/axiom.con", "- A1 <= E;\n");
		testAssumptions("tests/jif/axiom2.con", "- list (A) <= C;\n" ); 		
		testAssumptions("tests/jif/axiom3.con", "- Bool <= Char;Bool <= Collects2;Char <= Bool;Char <= Collects2;\n");		
		testAssumptions("tests/jif/axiom4.con", "- Bool <= Char;Char <= Bool;cons_2 (Char) (x) <= G;\n");
		testAssumptions("tests/jif/axiom5.con", "- list (Bool) <= Eq;\n");
		testErrorPaths("tests/jif/axiom6.con", false);
		testErrorPaths("tests/jif/axiom7.con", false);
		testAssumptions("tests/jif/associativity.con", "- (list (x))->(list (x)) <= list (CHAR);list (CHAR) <= (list (x))->(list (x));\n" );
		testAssumptions("tests/jif/extraEdges.con", "- b <= a;\n");
		testErrorPaths("tests/jif/infiType1.con", true);
		testErrorPaths("tests/jif/infiType2.con", true);
		testErrorPaths("tests/jif/infiType3.con", true);
		testErrorPaths("tests/jif/family.con", false);		
		testErrorPaths("tests/jif/family1.con", true);
		testErrorPaths("tests/jif/family2.con", false);
		testErrorPaths("tests/jif/family3.con", false);
//		testAssumptions("tests/jif/inte.con", "- b <= c;\n");
//		testAssumptions("/home/zhdf/workspace/LemonTool/error.con", "- a <= b\n");
	}
	
	@Test
	public void testHypothesis () {
		/* test for Jif constraint */
		
		/* auction */
		testAssumptions("tests/hypothesis/constraints/AirlineAgent1.con", "- (TheAirline)->(TheAirline) <= C_L;\n");// same assumption
		/* AirlineAgent2 secure */
		/* User1 secure */
		testAssumptions("tests/hypothesis/constraints/AirlineExample1.con", "- AirlineA <= airlines;\n");// same assumption
		testAssumptions("tests/hypothesis/constraints/AirlineExample2.con", "- AirlineA <= airlines;AirlineB <= airlines;\n");// same assumption
		/* jif compiler is too conservative in this case. it's secure */
		testAssumptions("tests/hypothesis/constraints/AirlineExample3.con", "The program passed program analysis. No errors were found."); // secure
		
		/* battleship */
		testAssumptions("tests/hypothesis/constraints/Board1.con", "- (p1)->(p1) <= C_L;(p1)<-(p1) <= I_L;C_L <= (p1)->(p1);I_L <= (p1)<-(p1);\n"); // same assumption

		/* social */
		/* SocialNetwork1 secure */
		testAssumptions("tests/hypothesis/constraints/SocialNetwork2.con", "- bb <= SN;bg <= SN;user <= SN;\n"); // same

		/* friendmap */
		testAssumptions("tests/hypothesis/constraints/Location1.con", "- C_L <= (C_A)join((⊥)->(⊥));\n");// same assumption
		testAssumptions("tests/hypothesis/constraints/Snapp1.con", "- owner2 <= store1;\n"); // same assumption
		/* Snapp2 secure */
		testAssumptions("tests/hypothesis/constraints/FriendMap1.con", "- C_*l <= (⊤)->(s1);\n"); // same assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap2.con", "- (⊤)->(user.friends) <= (⊤)->(worker$);\n"); // weaker assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap3.con", "- C_*map_update <= C_*map_access;\n"); // weaker assumption
		/* FriendMap4 secure */
		testAssumptions("tests/hypothesis/constraints/FriendMap5.con", "- C_*l <= (⊤)->(s1);\n"); // weaker assumption
		/* FriendMap6 secure */
		testAssumptions("tests/hypothesis/constraints/FriendMap7.con", "- C_l <= (⊤)->(n1);C_n <= (⊤)->(n1);\n"); // same assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap8.con", "- C_s <= C_*l;I_s <= I_*l;\n"); // weaker assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap9.con", "- C_*iterLabel <= (⊤)->(fn);\n"); //  weaker assumption, this is a good example
		testAssumptions("tests/hypothesis/constraints/FriendMap10.con", "- C_*lbl <= (⊤)->(n1);\n"); // weaker  assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap11.con", "- C_user <= (⊤)->(n1);\n"); // weaker  assumption
		/* FriendMap12 secure */
		/* FriendMap13 secure */
		testAssumptions("tests/hypothesis/constraints/FriendMap14.con", "- C_*iterLabel <= (⊤)->(fn);\n"); // weaker  assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap15.con", "C_*iterLabel <= (⊤)->(fn);\n"); // same  assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap16.con", "- (⊤)<-(localStore) <= I_*lbl;C_*lbl <= (⊤)->(localStore);\n"); // same  assumption
		testAssumptions("tests/hypothesis/constraints/FriendMap17.con", "- C_*l <= (⊤)->(s1);\n"); // same  assumption
		testAssumptions("tests/hypothesis/constraints/Box1.con", "- C_L <= (C_A)join((⊥)->(⊥));\n"); // same assumption
		/* Box2 secure */
		/* Box3 secure */
		testAssumptions("tests/hypothesis/constraints/Box4.con", "- C_*l <= C_*a;I_*l <= I_*a;\n"); // same assumption
		/* MapImage1 secure */
		testAssumptions("tests/hypothesis/constraints/MapImage2.con", "- C_A <= (⊤)->(s4);\n"); // same assumption
		testAssumptions("tests/hypothesis/constraints/MapImage3.con", "- C_*a <= (⊤)->(s1);\n"); // same assumption
		testAssumptions("tests/hypothesis/constraints/MapImage4.con", "- C_*bdry_update <= C_*bdry_access;I_*bdry_update <= I_*bdry_access;\n"); // same assumption
		testAssumptions("tests/hypothesis/constraints/MapImage5.con", "- C_boundary <= C_L;C_data <= C_L;I_boundary <= I_L;I_data <= I_L;\n"); // weaker assumption
		testAssumptions("tests/hypothesis/constraints/MapImage6.con", "- C_L <= C_*l;C_a <= C_*l;C_l <= C_*l;I_L <= I_*l;I_a <= I_*l;I_l <= I_*l;\n"); // weaker assumption (6 assumptions with integrity)
		testAssumptions("tests/hypothesis/constraints/MapServer1.con", "- C_*l <= (⊤)->(this.store);\n"); // same assumption
	}
	
	@Test
	public void testFriendMap () {
		testAssumptions("tests/friendmap/constraints/FriendMap3108_1.con", "(⊥)<-(⊥) <= I_*map_access");
		testAssumptions("tests/friendmap/constraints/FriendMap3110_1.con", "(⊤)<-(user.p) <= I_*map_update");
		testAssumptions("tests/friendmap/constraints/FriendMap3112_1.con", "(⊤)<-(local) <= I_*box_u");
		testAssumptions("tests/friendmap/constraints/FriendMap3113_1.con", "C_map <= C_*map_update;I_map <= I_*map_update;");
		testAssumptions("tests/friendmap/constraints/FriendMap3114_1.con", "C_*map_update <= (⊥)->(⊥)");
		testAssumptions("tests/friendmap/constraints/FriendMap3115_1.con", "C_*map_update <= (⊥)->(⊥)");
		testExpression("tests/friendmap/constraints/FriendMap3116_1.con", "FriendMap3116.fab:429,32-42");
		testExpression("tests/friendmap/constraints/FriendMap3120_1.con", "FriendMap3120.fab:494,4-5");
		testAssumptions("tests/friendmap/constraints/FriendMap3122_1.con", "(⊤)<-((fo)meet(fn)) <= I_*map_update");
		testExpression("tests/friendmap/constraints/FriendMap3144_1.con", "FriendMap3144.fab:445,32-42");
		testAssumptions("tests/friendmap/constraints/FriendMap3167_1.con", "C_l <= (⊤)->(n1);C_n <= (⊤)->(n1)");
		testAssumptions("tests/friendmap/constraints/FriendMap3192_1.con", "C_s <= C_*friend_access_bound");
		testAssumptions("tests/friendmap/constraints/FriendMap3193_1.con", "(⊤)<-(this.service) <= I_*fetchLabel");
	}
	
	@Test
	public void testJif () {
		testErrorPaths("tests/jiftestcases/A_1.con", false);
		testErrorPaths("tests/jiftestcases/A_2.con", false);
		testErrorPaths("tests/jiftestcases/A_3.con", false);
		testErrorPaths("tests/jiftestcases/A_4.con", false);
		
		testErrorPaths("tests/jiftestcases/Account_1.con", false);
		testErrorPaths("tests/jiftestcases/Account_2.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabel1_1.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabel1_2.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabel1_3.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabel2_1.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_1.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_2.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_3.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_4.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_5.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_6.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst_7.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabelSubst2_1.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst2_2.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst2_3.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst2_4.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabelSubst3_1.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst3_2.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst3_3.con", false);
		
		testErrorPaths("tests/jiftestcases/ArgLabelSubst4_1.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst4_2.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst4_3.con", false);
		testErrorPaths("tests/jiftestcases/ArgLabelSubst4_4.con", false);
		
		testErrorPaths("tests/jiftestcases/Array_1.con", false);
		testErrorPaths("tests/jiftestcases/Array_2.con", false);
		testErrorPaths("tests/jiftestcases/Array_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Array1_1.con", true);
		testErrorPaths("tests/jiftestcases/Array1_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array2_1.con", false);
		testErrorPaths("tests/jiftestcases/Array2_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array3_1.con", false);
		testErrorPaths("tests/jiftestcases/Array3_2.con", false);
		testErrorPaths("tests/jiftestcases/Array3_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Array4_1.con", false);
		testErrorPaths("tests/jiftestcases/Array4_2.con", false);
		testErrorPaths("tests/jiftestcases/Array4_3.con", false);
		testErrorPaths("tests/jiftestcases/Array4_4.con", false);
		testErrorPaths("tests/jiftestcases/Array4_5.con", false);
		
		testErrorPaths("tests/jiftestcases/Array5_1.con", false);
		testErrorPaths("tests/jiftestcases/Array5_2.con", false);
		testErrorPaths("tests/jiftestcases/Array5_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Array6_1.con", true);
		testErrorPaths("tests/jiftestcases/Array6_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array7_1.con", true);
		testErrorPaths("tests/jiftestcases/Array7_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array8_1.con", false);
		testErrorPaths("tests/jiftestcases/Array8_2.con", true);
		
		testErrorPaths("tests/jiftestcases/Array9_1.con", false);
		testErrorPaths("tests/jiftestcases/Array9_2.con", true);
		
		testErrorPaths("tests/jiftestcases/Array10_1.con", false);
		testErrorPaths("tests/jiftestcases/Array10_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array11_1.con", false);
		testErrorPaths("tests/jiftestcases/Array11_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array12_1.con", true);
		testErrorPaths("tests/jiftestcases/Array12_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array13_1.con", false);
		testErrorPaths("tests/jiftestcases/Array13_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array14_1.con", true);
		testErrorPaths("tests/jiftestcases/Array14_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array15_1.con", false);
		testErrorPaths("tests/jiftestcases/Array15_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array16_1.con", false);
		testErrorPaths("tests/jiftestcases/Array16_2.con", false);
		
		testErrorPaths("tests/jiftestcases/Array17_1.con", false);
		testErrorPaths("tests/jiftestcases/Array17_2.con", false);
		
		// Array18 is not constraint related
		
		testErrorPaths("tests/jiftestcases/Array19_1.con", false);
		testErrorPaths("tests/jiftestcases/Array19_2.con", false);
		testErrorPaths("tests/jiftestcases/Array19_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Array20_1.con", false);
		testErrorPaths("tests/jiftestcases/Array20_2.con", false);
		testErrorPaths("tests/jiftestcases/Array20_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn1_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn1_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn1_3.con", false);
		testErrorPaths("tests/jiftestcases/Dyn1_4.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn2_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn2_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn2_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn4_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn4_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn4_3.con", false);
		testErrorPaths("tests/jiftestcases/Dyn4_4.con", false);
		testErrorPaths("tests/jiftestcases/Dyn4_5.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn5_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn5_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn5_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn6_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn6_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn6_3.con", false);
		
		testErrorPaths("tests/jiftestcases/Dyn7_1.con", false);
		testErrorPaths("tests/jiftestcases/Dyn7_2.con", false);
		testErrorPaths("tests/jiftestcases/Dyn7_3.con", false);
		
		testErrorPaths("tests/jiftestcases/DynLabel1_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel1_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel1_3.con", false);
		
		// DynLabel2-7 fails due to syntactic errors
		
		testErrorPaths("tests/jiftestcases/DynLabel8_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel8_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel8_3.con", false);
		
		testErrorPaths("tests/jiftestcases/DynLabel9_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel9_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel9_3.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel9_4.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel9_5.con", false);
	
		testErrorPaths("tests/jiftestcases/DynLabel10_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel10_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel10_3.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel10_4.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel10_5.con", false);
		
		testErrorPaths("tests/jiftestcases/DynLabel11_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel11_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel11_3.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel11_4.con", false);
		
		testErrorPaths("tests/jiftestcases/DynLabel12_1.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel12_2.con", false);
		testErrorPaths("tests/jiftestcases/DynLabel12_3.con", true);
		testErrorPaths("tests/jiftestcases/DynLabel12_4.con", false);
		
		testErrorPaths("tests/jiftestcases/DynLabel13_1.con", true);
		testErrorPaths("tests/jiftestcases/DynLabel13_2.con", false);
		
		testErrorPaths("tests/jiftestcases/For1_1.con", false);
		testErrorPaths("tests/jiftestcases/For1_2.con", false);
		
		testErrorPaths("tests/jiftestcases/For2_1.con", true);
		testErrorPaths("tests/jiftestcases/For2_2.con", false);
		
		testErrorPaths("tests/jiftestcases/For3_1.con", true);
		testErrorPaths("tests/jiftestcases/For3_2.con", false);
		
		testErrorPaths("tests/jiftestcases/For4_1.con", true);
		testErrorPaths("tests/jiftestcases/For4_2.con", false);
		
		testErrorPaths("tests/jiftestcases/For5_1.con", true);
		testErrorPaths("tests/jiftestcases/For5_2.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint01_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint01_2.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint02_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint02_2.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint03_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint03_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint03_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint03_4.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint04_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint04_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint04_3.con", false);
	
		testErrorPaths("tests/jiftestcases/LabelLeConstraint05_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint05_2.con", true);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint05_3.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint06_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint06_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint06_3.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint07_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint07_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint07_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint07_4.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08_4.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08a_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08a_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08a_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08a_4.con", true);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint08a_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint09_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint09_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint09_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint09_4.con", true);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint09_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint10_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint10_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint10_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint10_4.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint10_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint11_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint11_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint11_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint11_4.con", true);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint11_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint12_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint12_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint12_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint12_4.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint12_5.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint13_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint13_2.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelLeConstraint14_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint14_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelLeConstraint14_3.con", false);
		
		testErrorPaths("tests/jiftestcases/LabelSubst01_1.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_2.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_3.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_4.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_5.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_6.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_7.con", false);
		testErrorPaths("tests/jiftestcases/LabelSubst01_8.con", false);
	}
	
	@Test
	public void testDowngrade () {
		
		// Tests from Battleship
		testConstraint("tests/downgrade/Board1.con", "Board1.jif:85,17-41");
		testConstraint("tests/downgrade/Board2.con", "Board2.jif:95,35-36");
		/*
		 * The following two examples produces a large suggest set since a lot
		 * of label variables are used on the unsatisfiable paths:
		 * 
		 * Source --> posX --> newCoordinate --> c --> newShip --> t --> error
		 */
		testConstraint("tests/downgrade/Board3.con", "Board3.jif:99,31-36");
		testConstraint("tests/downgrade/Board4.con", "Board4.jif:100,31-36");
		testConstraint("tests/downgrade/Board5.con", "Board5.jif:103,33-42");
		testConstraint("tests/downgrade/Board6.con", "Board6.jif:104,38-53");
		testConstraint("tests/downgrade/Main1.con", "Main1.jif:33,33-36");
		testConstraint("tests/downgrade/Player1.con", "Player1.jif:141,42-45");
		testConstraint("tests/downgrade/Player2.con", "Player2.jif:142,42-45");
		// now SHErrLoc reports 176,18-69, which is also correct. Check later
//		testConstraint("tests/downgrade/Player3.con", "Player3.jif:179,1-15");

		// Tests from Auction, Main.jif does not compile
		testConstraint("tests/downgrade/AirlineExample1.con", "AirlineExample1.jif:74,13-15");
		// I don't understand the error reported by Jif, seems the constraint file is error-free
//		testConstraint("tests/downgrade/AirlineExample2.con", "AirlineExample2.jif:71,51-57");
		
		// Tests from jpmail/jifcrypt
		testConstraint("tests/downgrade/jpmail/constraints/AESClosure_1.con", "AESClosure.jif:17,11-30");
		testConstraint("tests/downgrade/jpmail/constraints/DESClosure_1.con", "DESClosure.jif:27,11-30");
		testConstraint("tests/downgrade/jpmail/constraints/MD5Closure_1.con", "MD5Closure.jif:18,27-36");
		testConstraint("tests/downgrade/jpmail/constraints/RSAClosure_1.con", "RSAClosure.jif:20,11-30");
		testConstraint("tests/downgrade/jpmail/constraints/TripleDESClosure_1.con", "TripleDESClosure.jif:17,11-30");
		// RSA.jif is secure after removing all downgrades
		
		// Tests from jpmail
		testConstraint("tests/downgrade/jpmail/constraints/MailReaderCrypto_1.con", "MailReaderCrypto.jif:634,24-32");
	}
}
