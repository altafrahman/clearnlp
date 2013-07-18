/**
* Copyright 2013 IPSoft Inc.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.generation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import com.googlecode.clearnlp.constant.english.ENAux;
import com.googlecode.clearnlp.constant.english.ENModal;
import com.googlecode.clearnlp.constant.english.ENPronoun;
import com.googlecode.clearnlp.constant.english.ENPunct;
import com.googlecode.clearnlp.constant.universal.STConstant;
import com.googlecode.clearnlp.constituent.CTLibEn;
import com.googlecode.clearnlp.dependency.DEPArc;
import com.googlecode.clearnlp.dependency.DEPFeat;
import com.googlecode.clearnlp.dependency.DEPLib;
import com.googlecode.clearnlp.dependency.DEPLibEn;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLLib;
import com.googlecode.clearnlp.morphology.MPLibEn;

/**
 * Used for Eliza.
 * @since 1.4.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class LGAsk
{
	private final String USER    		= "user";
	private final String PLEASE    		= "please";
	private final String NON_FINITE		= "non-finite";
	private final String WH_NON_FINITE	= "wh-non-finite";
	
	private LGVerbEn g_verb;
	
	public LGAsk() {}
	
	public LGAsk(ZipInputStream inputStream)
	{
		g_verb = new LGVerbEn(inputStream);
	}
	
	/** Generates a declarative sentence with "ask" from a question. */
	public DEPTree generateAskFromQuestion(DEPTree tree)
	{
		tree = tree.clone();
		tree.setDependents();
		
		DEPNode root = tree.getFirstRoot();
		return (root == null) ? null : generateAskFromQuestionAux(tree, root);
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private DEPTree generateAskFromQuestionAux(DEPTree tree, DEPNode verb)
	{
		LGLibEn.convertFirstFormToLowerCase(tree);
		DEPNode ref = getReferentArgument(verb);
		
		if (ref == null || !ref.isLabel(DEPLibEn.P_SBJ))
			relocateAuxiliary(tree, verb);

		addPrefix(tree, verb, ref);
		convertYou(tree, verb);
		addPeriod(tree, verb);
		
		tree.resetIDs();
		tree.resetDependents();
		
		return tree;
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private DEPNode getReferentArgument(DEPNode verb)
	{
		DEPNode dep;
		
		for (DEPArc arc : verb.getDependents())
		{
			dep = arc.getNode();
			
			if (dep.containsSHead(verb, SRLLib.P_ARG_REF))
				return dep;
		}
		
		return null;
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void relocateAuxiliary(DEPTree tree, DEPNode verb)
	{
		List<DEPNode> auxes = new ArrayList<DEPNode>();
		DEPNode sbj = null;

		for (DEPArc arc : verb.getDependents())
		{
			if (arc.isLabel(DEPLibEn.P_AUX))
				auxes.add(arc.getNode());
			else if (arc.isLabel(DEPLibEn.P_SBJ))
				sbj = arc.getNode();
		}
		
		if (sbj != null)
		{
			if (!auxes.isEmpty() && auxes.get(0).id < sbj.id)
			{
				relocateAuxiliaryAux(tree, verb, auxes, sbj);
			}
			else if (verb.isLemma(ENAux.BE) && verb.id < sbj.id)
			{
				tree.remove(verb);
				tree.add(sbj.getLastNode().id, verb);
				setBeVerbForm(verb, sbj);
			}
		}
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void relocateAuxiliaryAux(DEPTree tree, DEPNode verb, List<DEPNode> auxes, DEPNode sbj)
	{
		DEPNode aux = auxes.get(0);
		tree.remove(aux);
		
		if (aux.isLemma(ENAux.DO))
		{
			if (auxes.size() > 1)
			{
				DEPNode node = auxes.get(1);
				
				if (MPLibEn.isVerb(node.pos))
					verb = node;
			}
			
			verb.pos = aux.pos;
			
			if (aux.isPos(CTLibEn.POS_VBD))
				verb.form = g_verb.getPastForm(verb.lemma);
			else if (aux.isPos(CTLibEn.POS_VBZ))
				verb.form = LGVerbEn.get3rdSingularForm(verb.lemma);
			else if (aux.isPos(CTLibEn.POS_VBP) && sbj.isLemma(ENPronoun.YOU))
			{
				verb.form = LGVerbEn.get3rdSingularForm(verb.lemma);
				verb.pos  = CTLibEn.POS_VBZ;
			}
		}
		else
		{
			tree.add(sbj.getLastNode().id, aux);
			
			if (aux.isLemma(ENAux.BE))
				setBeVerbForm(aux, sbj);
			else if (aux.isLemma(ENAux.HAVE))
				set3rdSingularVerbForm(aux, sbj);
		}
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void setBeVerbForm(DEPNode verb, DEPNode sbj)
	{
		if (sbj.isLemma(ENPronoun.YOU))
		{
			if (verb.isPos(CTLibEn.POS_VBD))
				verb.form = ENAux.WAS;
			else if (verb.isPos(CTLibEn.POS_VBP))
			{
				verb.form = ENAux.IS;
				verb.pos  = CTLibEn.POS_VBZ;
			}
		}
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void set3rdSingularVerbForm(DEPNode verb, DEPNode sbj)
	{
		if (sbj.isLemma(ENPronoun.YOU))
		{
			if (verb.isPos(CTLibEn.POS_VBP))
			{
				verb.form = LGVerbEn.get3rdSingularForm(verb.lemma);
				verb.pos  = CTLibEn.POS_VBZ;
			}
		}
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void addPrefix(DEPTree tree, DEPNode verb, DEPNode ref)
	{
		DEPNode ask = getNode(tree.get(0), "Ask", "ask", CTLibEn.POS_VB, DEPLibEn.DEP_ROOT, null);
		verb.setHead(ask);
		tree.add(1, ask);
		
		if (ref == null && !hasRelativizer(tree))
		{
			DEPNode complm = getNode(verb, "whether", "whether", CTLibEn.POS_IN, DEPLibEn.DEP_COMPLM, null);
			tree.add(2, complm);			
		}
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private void convertYou(DEPTree tree, DEPNode head)
	{
		if (head.isLemma(ENPronoun.YOU) || head.isLemma(ENPronoun.YOURSELF))
		{
			head.form = head.lemma = USER;
			head.pos  = CTLibEn.POS_NN;
			tree.add(tree.indexOf(head), getNode(head, STConstant.THE, STConstant.THE, CTLibEn.POS_DT, DEPLibEn.DEP_DET, null));
		}
		else if (head.isLemma(ENPronoun.YOUR) || head.isLemma(ENPronoun.YOURS))
		{
			int idx = tree.indexOf(head);
			
			head.form = head.lemma = USER;
			head.pos  = CTLibEn.POS_NN;
			
			tree.add(idx  , getNode(head, STConstant.THE, STConstant.THE, CTLibEn.POS_DT, DEPLibEn.DEP_DET, null));
			tree.add(idx+2, getNode(head, STConstant.APOSTROPHE_S, STConstant.APOSTROPHE_S, CTLibEn.POS_POS, DEPLibEn.DEP_POSSESSIVE, null));
		}
		
		for (DEPArc arc : head.getDependents())
			convertYou(tree, arc.getNode());
	}
	
	private void addPeriod(DEPTree tree, DEPNode root)
	{
		DEPNode last = tree.get(tree.size()-1);
		
		if (last.isPos(CTLibEn.POS_PERIOD))
			last.form = last.lemma = ENPunct.PERIOD;
		else
			tree.add(getNode(root, ENPunct.PERIOD, ENPunct.PERIOD, CTLibEn.POS_PERIOD, DEPLibEn.DEP_PUNCT, null));
	}
	
	/** {@link LGAsk#generateAskFromQuestion(DEPTree, String)}. */
	private boolean hasRelativizer(DEPTree tree)
	{
		int i, size = tree.size();
		DEPNode node;
		
		for (i=1; i<size; i++)
		{
			node = tree.get(i);
			
			if (node.containsSHead(SRLLib.P_ARG_REF))
				return true;
		}
		
		return false;
	}
	
	/** Generates a question from a declarative sentence with "ask". */
	public DEPTree generateQuestionFromAsk(DEPTree tree)
	{
		tree = tree.clone();
		tree.setDependents();
		
		DEPNode root = tree.getFirstRoot();
		if (root == null)	return null;
		DEPNode dep;
		
		for (DEPArc arc : root.getDependents())
		{
			dep = arc.getNode();
			
			if (MPLibEn.isVerb(dep.pos))
				return generateQuestion(dep);
		}
		
		return null;
	}
	
	/** Generates a question from a declarative sentence. */
	public DEPTree generateQuestionFromDeclarative(DEPTree tree, boolean convertUnI)
	{
		tree = tree.clone();
		tree.setDependents();
		
		LGLibEn.convertFirstFormToLowerCase(tree);
		if (convertUnI)  LGLibEn.convertUnI(tree);
		
		DEPNode root = tree.getFirstRoot();
		if (root == null)	return null;
		
		return generateQuestion(root);
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	public DEPTree generateQuestion(DEPNode verb)
	{
		Set<DEPNode> added = new HashSet<DEPNode>();
		DEPTree tree = new DEPTree();
		DEPNode rel, aux;
		
		rel = setRelativizer(tree, verb, added);
		aux = setAuxiliary(tree, verb, added, rel);
		setRest(tree, verb, added);
		resetDEPTree(tree, verb);
		if (aux != null) matchNumber(verb, aux);
		
		return tree;
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private DEPNode setRelativizer(DEPTree tree, DEPNode verb, Set<DEPNode> added)
	{
		DEPNode dep, rel, head;
		
		for (DEPArc arc : verb.getDependents())
		{
			dep = arc.getNode();
			rel = DEPLibEn.getRefDependentNode(dep);
			
			if (rel != null)
			{
				if (verb.id < rel.id)
				{
					head = rel.getHead();
					
					while (head != verb && !head.isPos(CTLibEn.POS_IN) && !MPLibEn.isVerb(head.pos))
					{
						rel  = head;
						head = head.getHead();
					}
				}
				else
				{
					head = rel.getHead();
					
					while (head != verb && head.id < verb.id)
					{
						rel  = head;
						head = head.getHead();
					}
				}

				addSubtree(tree, rel, added);
				return rel;
			}
		}
		
		return null;
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private DEPNode setAuxiliary(DEPTree tree, DEPNode verb, Set<DEPNode> added, DEPNode rel)
	{
		if (rel != null && DEPLibEn.P_SBJ.matcher(rel.getLabel()).find())
			return null;

		DEPNode dep;
		
		for (DEPArc arc : verb.getDependents())
		{
			dep = arc.getNode();
			
			if (arc.isLabel(DEPLibEn.P_AUX) && !dep.isPos(CTLibEn.POS_TO))
			{
				if (dep.isLemma(ENAux.GET))
					return addDoAuxiliary(tree, dep);
				else
				{
					addSubtree(tree, dep, added);
					return dep;
				}
			}
		}

		if (verb.isLabel(DEPLibEn.DEP_XCOMP))
		{
			toNonFinite(verb);
			
			if (rel != null)
			{
				dep = getNode(verb, ENModal.SHOULD, ENModal.SHOULD, CTLibEn.POS_MD, DEPLibEn.DEP_AUX, SRLLib.ARGM_MOD);
				tree.add(dep);
				tree.add(getNode(verb, ENPronoun.I, ENPronoun.I, CTLibEn.POS_PRP, DEPLibEn.DEP_NSUBJ, SRLLib.ARG0));
				verb.addFeat(DEPLib.FEAT_VERB_TYPE, WH_NON_FINITE);
				return dep;
			}
			else
			{
				verb.addFeat(DEPLib.FEAT_VERB_TYPE, NON_FINITE);
				return null;
			}
		}
		else if (verb.isLemma(ENAux.BE))
		{
			tree .add(verb);
			added.add(verb);
			return verb;
		}
		else
			return addDoAuxiliary(tree, verb);
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private DEPNode addDoAuxiliary(DEPTree tree, DEPNode verb)
	{
		DEPNode aux;
		
		if (verb.isPos(CTLibEn.POS_VBZ))
			tree.add(aux = getNode(verb, ENAux.DOES, ENAux.DO, verb.pos, DEPLibEn.DEP_AUX, null));
		else if (verb.isPos(CTLibEn.POS_VBD) || verb.isPos(CTLibEn.POS_VBN))
			tree.add(aux = getNode(verb, ENAux.DID , ENAux.DO, CTLibEn.POS_VBD, DEPLibEn.DEP_AUX, null));
		else
			tree.add(aux = getNode(verb, ENAux.DO  , ENAux.DO, verb.pos, DEPLibEn.DEP_AUX, null));
		
		toNonFinite(verb);
		return aux;
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void setRest(DEPTree tree, DEPNode verb,  Set<DEPNode> added)
	{
		for (DEPNode node : verb.getSubNodeSortedList())
		{
			if (added.contains(node))
				continue;
			else if (node.isDependentOf(verb) && (node.isPos(CTLibEn.POS_TO) || node.isLabel(DEPLibEn.DEP_COMPLM) || node.isLabel(DEPLibEn.DEP_MARK)))
				continue;
			else
				tree.add(node);
		}
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void resetDEPTree(DEPTree tree, DEPNode root)
	{
		List<DEPNode> remove = new ArrayList<DEPNode>();
		convertYou(root, remove);
		tree.removeAll(remove);
		
		resetDEPTreePost(tree, root);
		tree.resetIDs();
		tree.resetDependents();
	}

	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void resetDEPTreePost(DEPTree tree, DEPNode root)
	{
		root.setHead(tree.get(0), DEPLibEn.DEP_ROOT);
		String end = ENPunct.QUESTION_MARK;
		String vtype;
		
		if ((vtype = root.getFeat(DEPLib.FEAT_VERB_TYPE)) != null && vtype.equals(NON_FINITE))
		{
			tree.add(1, getNode(root, PLEASE, PLEASE, CTLibEn.POS_UH, DEPLibEn.DEP_INTJ, SRLLib.ARGM_DIS));
			end = ENPunct.PERIOD;
		}
		
		DEPNode last = root.getLastNode();
		
		if (last.isPos(CTLibEn.POS_PERIOD))
		{
			last.form  = end;
			last.lemma = end;
		}
		else
			tree.add(getNode(root, end, end, CTLibEn.POS_PERIOD, DEPLibEn.DEP_PUNCT, null));
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void addSubtree(DEPTree tree, DEPNode head, Set<DEPNode> added)
	{
		List<DEPNode> list = head.getSubNodeSortedList();
		
		tree .addAll(list);
		added.addAll(list);
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void toNonFinite(DEPNode verb)
	{
		verb.form = verb.lemma;
		verb.pos  = CTLibEn.POS_VB;
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private DEPNode getNode(DEPNode head, String form, String lemma, String pos, String deprel, String label)
	{
		DEPNode aux = new DEPNode(0, form, lemma, pos, new DEPFeat());
		aux.initXHeads();
		aux.initSHeads();
		
		aux.setHead(head, deprel);
		if (label != null)	aux.addSHead(head, label);

		return aux;
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void matchNumber(DEPNode verb, DEPNode aux)
	{
		for (DEPArc arc : verb.getDependents())
		{
			if (arc.isLabel(DEPLibEn.P_SBJ))
			{
				DEPNode dep = arc.getNode();
				
				if (dep.isLemma(ENPronoun.YOU))
				{
					if (aux.isLemma(ENAux.DO))
					{
						if (!aux.isPos(CTLibEn.POS_VBD) && !aux.isPos(CTLibEn.POS_VBN))
						{
							aux.form = ENAux.DO;
							aux.pos  = CTLibEn.POS_VBP;
						}
					}
					else if (aux.isLemma(ENAux.BE))
					{
						if (aux.isPos(CTLibEn.POS_VBD) || aux.isPos(CTLibEn.POS_VBN))
						{
							aux.form = ENAux.WERE;
							aux.pos  = CTLibEn.POS_VBD;
						}
						else
						{
							aux.form = ENAux.ARE;
							aux.pos  = CTLibEn.POS_VBP;
						}
					}
					else if (aux.isLemma(ENAux.HAVE))
					{
						if (!aux.isPos(CTLibEn.POS_VBD) && !aux.isPos(CTLibEn.POS_VBN))
						{
							aux.form = ENAux.HAVE;
							aux.pos  = CTLibEn.POS_VBP;
						}
					}
				}
				
				break;
			}
		}
	}
	
	/** Called by {@link LGAsk#generateQuestionFromAsk(DEPTree, String)}. */
	private void convertYou(DEPNode node, List<DEPNode> remove)
	{
		List<DEPArc> deps = node.getDependents();
		
		if (node.isPos(CTLibEn.POS_PRPS) && !ENPronoun.is1stSingular(node.lemma))
		{
			node.form = node.lemma = ENPronoun.YOUR;
		}
		else if (node.isLabel(DEPLibEn.DEP_POSS) && node.isLemma(USER))
		{
			node.form = node.lemma = ENPronoun.YOUR;
			node.pos  = CTLibEn.POS_PRPS;
			remove.addAll(node.getDependentNodes());
		}
		else if (node.isPos(CTLibEn.POS_PRP) && !ENPronoun.is1stSingular(node.lemma))
		{
			if (node.lemma.endsWith("self"))
				node.form = node.lemma = ENPronoun.YOURSELF;
			else if (node.lemma.endsWith("s"))
				node.form = node.lemma = ENPronoun.YOURS;
			else
				node.form = node.lemma = ENPronoun.YOU;
		}
		else if (node.isLemma(USER))
		{
			node.form = node.lemma = ENPronoun.YOU;
			node.pos  = CTLibEn.POS_PRP;
			remove.addAll(node.getDependentNodes());
		}
		else if (!deps.isEmpty())
		{
			DEPNode poss = node.getFirstDependentByLabel(DEPLibEn.DEP_POSS);
			boolean hasPoss = (poss != null) && poss.isLemma(USER);
			DEPNode dep;
			
			for (DEPArc arc : deps)
			{
				dep = arc.getNode();
				
				if (hasPoss && arc.isLabel(DEPLibEn.DEP_DET))
					remove.add(dep);
				else
					convertYou(dep, remove);
			}
			
			deps.removeAll(remove);
		}
	}
}
