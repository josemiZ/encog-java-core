/*
 * Encog(tm) Core v3.2 - Java Version
 * http://www.heatonresearch.com/encog/
 * http://code.google.com/p/encog-java/
 
 * Copyright 2008-2012 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package org.encog.neural.neat.training;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.encog.EncogError;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.mathutil.randomize.RangeRandomizer;
import org.encog.neural.neat.NEATNeuronType;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.networks.training.TrainingError;

/**
 * Implements a NEAT innovation list.
 * 
 * NeuroEvolution of Augmenting Topologies (NEAT) is a genetic algorithm for the
 * generation of evolving artificial neural networks. It was developed by Ken
 * Stanley while at The University of Texas at Austin.
 * 
 * http://www.cs.ucf.edu/~kstanley/
 * 
 */
public class NEATInnovationList implements Serializable {

	/**
	 * Serial id.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The next neuron id.
	 */
	private long nextNeuronID = 0;

	/**
	 * The population.
	 */
	private NEATPopulation population;

	/**
	 * The list of innovations.
	 */
	private Map<String, NEATInnovation> list = new HashMap<String, NEATInnovation>();

	/**
	 * The default constructor, used mainly for persistance.
	 */
	public NEATInnovationList() {

	}

	public static String produceKeyNeuron(long id) {
		StringBuilder result = new StringBuilder();
		result.append("n:");
		result.append(id);
		return result.toString();
	}

	public static String produceKeyNeuronSplit(long fromID, long toID) {
		StringBuilder result = new StringBuilder();
		result.append("ns:");
		result.append(fromID);
		result.append(":");
		result.append(toID);
		return result.toString();
	}

	public static String produceKeyLink(long fromID, long toID) {
		StringBuilder result = new StringBuilder();
		result.append("l:");
		result.append(fromID);
		result.append(":");
		result.append(toID);
		return result.toString();
	}

	/**
	 * Construct an innovation list, that includes the initial innovations.
	 * 
	 * @param population
	 *            The population.
	 * @param links
	 *            The links.
	 * @param neurons
	 *            THe neurons.
	 */
	public NEATInnovationList(final NEATPopulation population) {

		this.population = population;

		this.findInnovation(0, NEATNeuronType.Bias);

		for (int i = 0; i < population.getInputCount(); i++) {
			this.findInnovation(1 + i, NEATNeuronType.Input);
		}

		for (int i = 0; i < population.getOutputCount(); i++) {
			this.findInnovation(1 + population.getInputCount() + i,
					NEATNeuronType.Output);
		}
		
		for (long fromID = 0; fromID < this.population.getInputCount() + 1; fromID++) {
			for (long toID = 0; toID < this.population.getOutputCount(); toID++) {
				findInnovation(fromID, toID);
			}
		}
		
		
		
	}

	/**
	 * Find an innovation for a hidden neuron that split a existing link. This
	 * is the means by which hidden neurons are introduced in NEAT.
	 * 
	 * @param neuronID
	 *            The source neuron ID in the link.
	 * @param toID
	 *            The target neuron ID in the link.
	 * @return The newly created innovation, or the one that matched the search.
	 */
	public NEATInnovation findInnovationSplit(long fromID, long toID) {
		String key = NEATInnovationList.produceKeyNeuronSplit(fromID, toID);

		synchronized (this.list) {
			if (this.list.containsKey(key)) {
				return this.list.get(key);
			} else {
				long neuronID = this.population.assignGeneID();
				NEATInnovation innovation = new NEATInnovation();
				innovation.setFromNeuronID(fromID);
				innovation
						.setInnovationID(this.population.assignInnovationID());
				innovation.setInnovationType(NEATInnovationType.NewNeuron);
				innovation.setNeuronID(neuronID);
				innovation.setNeuronType(NEATNeuronType.Hidden);
				innovation.setToNeuronID(toID);
				list.put(key, innovation);
				
				// create other sides of split, if needed
				findInnovation(fromID,neuronID);
				findInnovation(neuronID,toID);
				return innovation;
			}
		}
	}

	/**
	 * Find an innovation for a single neuron. Single neurons were created
	 * without producing a split. This means, the only single neurons are the
	 * input, bias and output neurons.
	 * 
	 * @param neuronID
	 *            The neuron ID to find.
	 * @return The newly created innovation, or the one that matched the search.
	 */
	public NEATInnovation findInnovation(long neuronID, NEATNeuronType t) {
		String key = NEATInnovationList.produceKeyNeuron(neuronID);

		synchronized (this.list) {
			if (this.list.containsKey(key)) {
				return this.list.get(key);
			} else {
				NEATInnovation innovation = new NEATInnovation();
				innovation.setFromNeuronID(-1);
				innovation
						.setInnovationID(this.population.assignInnovationID());
				innovation.setInnovationType(NEATInnovationType.NewNeuron);
				innovation.setNeuronID(neuronID);
				innovation.setNeuronType(t);
				innovation.setToNeuronID(-1);
				list.put(key, innovation);
				return innovation;
			}
		}
	}

	/**
	 * Find an innovation for a new link added between two existing neurons.
	 * 
	 * @param fromID
	 *            The source neuron ID in the link.
	 * @param toID
	 *            The target neuron ID in the link.
	 * @return The newly created innovation, or the one that matched the search.
	 */
	public NEATInnovation findInnovation(long fromID, long toID) {
		String key = NEATInnovationList.produceKeyNeuronSplit(fromID, toID);

		synchronized (this.list) {
			if (this.list.containsKey(key)) {
				return this.list.get(key);
			} else {
				NEATInnovation innovation = new NEATInnovation();
				innovation.setFromNeuronID(fromID);
				innovation
						.setInnovationID(this.population.assignInnovationID());
				innovation.setInnovationType(NEATInnovationType.NewLink);
				innovation.setNeuronID(-1);
				innovation.setNeuronType(NEATNeuronType.Hidden);
				innovation.setToNeuronID(toID);
				list.put(key, innovation);
				return innovation;
			}
		}
	}

	public void setPopulation(NEATPopulation population) {
		this.population = population;
	}

	public void setNextNeuronID(int l) {
		this.nextNeuronID = l;
	}

	/**
	 * @return A list of innovations.
	 */
	public Map<String, NEATInnovation> getInnovations() {
		return list;
	}
}
