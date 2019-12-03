package group20;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.timeline.Timeline;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

public class Agent20 extends AbstractNegotiationParty {

	private Bid opponentbid = null;
	private double time;
	int issueSize = 0;
	boolean isNashPoint = true;
	double myUtilityAtNP = 0;
	double opponentUtilityAtNP = 0;
	double myUtilityAtLK = 0;
	double opponentUtilityAtLK = 0;
	DistributionOpponent distributionOpponent;
	AdditiveUtilitySpace additiveUtilitySpace;
	List<Issue> issues;

	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
		additiveUtilitySpace = (AdditiveUtilitySpace) estimateUtilitySpace();
		;
		issues = additiveUtilitySpace.getDomain().getIssues();
		distributionOpponent = new DistributionOpponent(issues);
	}

	@Override
	public String getDescription() {
		return "group 20";
	}

	@Override
	public void receiveMessage(AgentID sender, Action act) {
		super.receiveMessage(sender, act);
		if (act instanceof Offer) { // sender is making an offer
			Offer offer = (Offer) act;
			this.opponentbid = offer.getBid();
			distributionOpponent.receiveBid(opponentbid, getTimeLine().getTime());
		}
	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> list) {
		this.issueSize = issues.size();
		this.time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
		int nextOfferChoice[] = new int[this.issueSize];
		if (opponentbid == null) {
			Bid bid = getNewBid(nextOfferChoice);
			return new Offer(this.getPartyId(), bid);
		} else {
			double myUtility = getEstimatedUtility(opponentbid);
			System.out.println("myUtility: " + myUtility);
			nextOfferChoice = nashPoint(this.issueSize);
			if (time > 0.5 && myUtility > this.myAcceptThreshold()
					&& myUtility > distributionOpponent.getBidsUtility(opponentbid)) {
				return new Accept(this.getPartyId(), opponentbid);
			} else if (this.time > 0.7 && myUtility > this.myAcceptThreshold()) {
				return new Accept(this.getPartyId(), opponentbid);
			} else if (this.time < 0.99) {
				Bid bid = getNewBid(nextOfferChoice);
				return new Offer(this.getPartyId(), bid);
			} else {
				return new EndNegotiation(this.getPartyId());
			}
		}
	}

	private double myAcceptThreshold() {
		double acceptThreshold = 1;
		if (this.time < 0.01) {
			acceptThreshold = 0.999;
		} else if (this.time < 0.02) {
			acceptThreshold = 0.99;
		} else if (this.time < 0.2) {
			acceptThreshold = 0.9;
		} else if (this.time < 0.5 && isNashPoint && this.myUtilityAtNP >= this.opponentUtilityAtNP) {
			acceptThreshold = myUtilityAtNP;
		} else if (isNashPoint && this.myUtilityAtNP >= this.opponentUtilityAtNP) {
			acceptThreshold = (myUtilityAtNP + opponentUtilityAtNP) / 2;
		} else if (this.time < 0.7 && isNashPoint && this.myUtilityAtNP < this.opponentUtilityAtNP) {
			acceptThreshold = (myUtilityAtNP + opponentUtilityAtNP) / 2;
		} else if (isNashPoint && this.myUtilityAtNP < this.opponentUtilityAtNP) {
			acceptThreshold = myUtilityAtNP;
		} else if (this.time < 0.7 && isNashPoint == false && this.myUtilityAtLK >= this.opponentUtilityAtLK) {
			acceptThreshold = myUtilityAtLK + 0.05 - 0.1 * time;
		} else if (isNashPoint == false && this.myUtilityAtLK >= this.opponentUtilityAtLK) {
			acceptThreshold = (myUtilityAtLK + opponentUtilityAtLK) / 2;
		} else if (this.time < 0.7 && isNashPoint == false && this.myUtilityAtLK < this.opponentUtilityAtLK) {
			acceptThreshold = (myUtilityAtLK + opponentUtilityAtLK) / 2;
		} else if (isNashPoint == false && this.myUtilityAtLK < this.opponentUtilityAtLK) {
			acceptThreshold = myUtilityAtLK + 0.09 - 0.1 * time;
		}
		return acceptThreshold;
	}

	private double newOfferThreshold() {
		double newOfferThreshold = 1;
		if (time < 0.2) {
			newOfferThreshold = 0.5;
		} else if (time < 0.5) {
			newOfferThreshold = 0.8;
		} else {
			//newOfferThreshold = 0.8;
			if (isNashPoint) {
				newOfferThreshold = myAcceptThreshold();
			} else if (isNashPoint == false) {
				newOfferThreshold = myAcceptThreshold() + 0.1 - 0.1 * time;
			}

		}
		return newOfferThreshold;
	}

	public HashMap<Integer, double[]> getopponentEvaluation(ArrayList<HashMap<ValueDiscrete, Double>> valuesOfOptions) {
		HashMap<Integer, double[]> opponentEvaluation = new HashMap<Integer, double[]>();
		HashMap<ValueDiscrete, Double> opEval = new HashMap<ValueDiscrete, Double>();
		int i = 0;
		for (Issue issue : issues) {
			opEval = valuesOfOptions.get(i);
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			double[] optionEvaluation = new double[opEval.size()];
			int option = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				optionEvaluation[option] = valuesOfOptions.get(i).get(valueDiscrete);
				option++;
			}
			opponentEvaluation.put(i, optionEvaluation);
			i++;
		}
		return opponentEvaluation;
	}

	private int[] nashPoint(int issueSize) {
		double myWeight[] = new double[issueSize];
		Map<Integer, double[]> myEvaluation = new HashMap<Integer, double[]>();
		double opponentWeight[] = new double[issueSize];
		Map<Integer, double[]> opponentEvaluation = new HashMap<Integer, double[]>();
		int nashOption[] = new int[issueSize]; // the best option for each issue
		double myEvalofIssueAtNP[] = new double[issueSize];
		// double opponentEvalofIssueAtNP[] = new double [issueSize];
		int totalOption = 0;
		double nashDifference = 0;

		ArrayList<Double> opWeight = new ArrayList<Double>();
		opWeight = distributionOpponent.getWeights();
		opponentEvaluation = getopponentEvaluation(distributionOpponent.getEvaluation());

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			myWeight[issueNumber - 1] = additiveUtilitySpace.getWeight(issueNumber);
			opponentWeight[issueNumber - 1] = opWeight.get(issueNumber - 1);

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			double[] optionEvalution = new double[issueDiscrete.getNumberOfValues()];
			int optionNumber = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				try {
					optionEvalution[optionNumber] = evaluatorDiscrete.getEvaluation(valueDiscrete);
				} catch (Exception e) {
					e.printStackTrace();
				}
				optionNumber += 1;
			}
			myEvaluation.put(issueNumber - 1, optionEvalution);
			totalOption += optionNumber;
			// find the option with highest evaluation
			double myMax = 0;
			for (int j = 0; j < optionEvalution.length; j++) {
				if (optionEvalution[j] > myMax) {
					myEvalofIssueAtNP[issueNumber - 1] = optionEvalution[j];
					myMax = optionEvalution[j];
				}
				/*
				 * if (opponentEvaluation.get(issueNumber-1)[j]==1) {
				 * opponentEvalofIssueAtNP[issueNumber-1]=opponentEvaluation.get(issueNumber-1)[
				 * j]; }
				 */
			}
		}

		double issueDifference[] = new double[totalOption];
		double myChange[] = new double[totalOption];
		double opponentChange[] = new double[totalOption];
		int di = 0;
		int indexofIssue[] = new int[issueSize + 1];
		for (int i = 0; i < myWeight.length; i++) {
			double IssueUtility[] = new double[myEvaluation.get(i).length];
			double myIssueUtility[] = new double[myEvaluation.get(i).length];
			double opponentIssueUtility[] = new double[myEvaluation.get(i).length];
			indexofIssue[i] = di;

			for (int j = 0; j < myEvaluation.get(i).length; j++) {
				myIssueUtility[j] = myWeight[i] * myEvaluation.get(i)[j] / myEvalofIssueAtNP[i];
				opponentIssueUtility[j] = opponentWeight[i] * opponentEvaluation.get(i)[j];
				IssueUtility[j] = myIssueUtility[j] + opponentIssueUtility[j];
			}
			// find the nash point option for each issue
			double max = 0;
			for (int j = 0; j < IssueUtility.length; j++) {
				if (IssueUtility[j] > max) {
					nashOption[i] = j;
					max = IssueUtility[j];
				}
			}
			int dii = di;
			for (int k = 0; k < IssueUtility.length; k++) {
				// if the agents chose another point(not nash) , the changes of utility
				myChange[k + dii] = myIssueUtility[k] - myIssueUtility[nashOption[i]];
				opponentChange[k + dii] = opponentIssueUtility[k] - opponentIssueUtility[nashOption[i]];
				issueDifference[k + dii] = myChange[k + dii] + opponentChange[k + dii];
				di++;
			}
		}
		indexofIssue[issueSize] = issueDifference.length + 1;

		// calculate my and opponent's utility at the best option bid
		this.myUtilityAtNP = 0;
		this.opponentUtilityAtNP = 0;
		for (int i = 0; i < myWeight.length; i++) {
			this.myUtilityAtNP += myWeight[i] * myEvaluation.get(i)[nashOption[i]] / myEvalofIssueAtNP[i];
			this.opponentUtilityAtNP += opponentWeight[i] * opponentEvaluation.get(i)[nashOption[i]];
		}
		System.out.println(myUtilityAtNP + "-NASH-" + opponentUtilityAtNP);
		nashDifference = myUtilityAtNP - opponentUtilityAtNP;

		if (Math.abs(nashDifference) > 0.1) {
			int laikaChoice = 0;
			int laikaOption[] = new int[issueSize];
			laikaOption = nashOption;

			double min = 1;
			for (int i = 0; i < issueDifference.length; i++) {
				issueDifference[i] = Math.abs(nashDifference - issueDifference[i]);
				if (nashDifference > 0) {
					if (myChange[i] < 0 && opponentChange[i] > 0) {
						if (issueDifference[i] < min) {
							laikaChoice = i;
							min = issueDifference[i];
						}
					}
				} else {
					if (opponentChange[i] < 0 && myChange[i] > 0) {
						if (issueDifference[i] < min) {
							laikaChoice = i;
							min = issueDifference[i];
						}
					}
				}
			}

			for (int j = 0; j < indexofIssue.length; j++) {
				if (laikaChoice - indexofIssue[j] < 0) {
					laikaOption[j - 1] = laikaChoice - indexofIssue[j - 1];
					break;
				}
			}
			this.myUtilityAtLK = 0;
			this.opponentUtilityAtLK = 0;
			for (int i = 0; i < myWeight.length; i++) {
				// System.out.println(laikaOption[i]);
				this.myUtilityAtLK += myWeight[i] * myEvaluation.get(i)[laikaOption[i]] / myEvalofIssueAtNP[i];
				this.opponentUtilityAtLK += opponentWeight[i] * opponentEvaluation.get(i)[laikaOption[i]];
			}
			System.out.println(myUtilityAtLK + "-LK-" + opponentUtilityAtLK);
			isNashPoint = false;
			return laikaOption;
		} else {
			isNashPoint = true;
			return nashOption;
		}
	}

	public Bid getNewBid(int[] nextOfferChoice) {
		double myWeight[] = new double[issues.size()];
		Map<Integer, double[]> myEvaluation = new HashMap<Integer, double[]>();

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			myWeight[issueNumber - 1] = additiveUtilitySpace.getWeight(issueNumber);

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			double[] optionEvalution = new double[issueDiscrete.getNumberOfValues()];
			int optionNumber = 0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				try {
					optionEvalution[optionNumber] = evaluatorDiscrete.getEvaluation(valueDiscrete);
				} catch (Exception e) {
					e.printStackTrace();
				}
				optionNumber += 1;
			}
			myEvaluation.put(issueNumber - 1, optionEvalution);
		}
		
		Bid bestBid = getRandomBidAboveThreshold(myWeight, myEvaluation, newOfferThreshold());
		return bestBid;
 
		// New bid method		
		/*
		 * if (this.time < 0.9) { Bid bestBid = getRandomBidAboveThreshold(myWeight,
		 * myEvaluation, newOfferThreshold()); return bestBid; } else { return
		 * getNashLaikaBid(nextOfferChoice); }
		 */
		 
	}

	private Bid getNashLaikaBid(int[] nextOfferChoice) {
		HashMap<Integer, Value> values = new HashMap<Integer, Value>();
		int issueIndex = 0;
		for (Issue currentIssue : utilitySpace.getDomain().getIssues()) {
			IssueDiscrete issueDiscrete = (IssueDiscrete) currentIssue;
			Value value = issueDiscrete.getValue(nextOfferChoice[issueIndex]);

			values.put(currentIssue.getNumber(), value);
			issueIndex++;
		}
		Bid bid = new Bid(utilitySpace.getDomain(), values);
		return bid;
	}

	public Bid getRandomBidAboveThreshold(double[] myWeight, Map<Integer, double[]> myEvaluation, double Threshold) {
		double utilityOfBid = 0;
		HashMap<Integer, Bid> rdBid = new HashMap<Integer, Bid>();
		Bid Bid = null;
		boolean bidLimit = false;
		int i=0;
		do {
			Bid = generateRandomBid();
			rdBid.put(i, Bid);
			utilityOfBid = getEstimatedUtility(Bid);
			i++;
			if(utilityOfBid>Threshold) {
				bidLimit = true;
				break;
			}
		} while (i < 100);
		
		if(bidLimit) {
			return Bid;
		}else {
			double maxUtility=0;
			for (Integer key : rdBid.keySet()) {
				double bidUtility=getEstimatedUtility(rdBid.get(key));
				if(bidUtility>maxUtility) {
					Bid = rdBid.get(key);
					maxUtility = bidUtility;
				}
			}
			return Bid;
		}
	}

	@Override
	public AbstractUtilitySpace estimateUtilitySpace() {
		Domain domain = userModel.getDomain();
		AdditiveUtilitySpaceFactory factory = new AdditiveUtilitySpaceFactory(domain);
		BidRanking br = userModel.getBidRanking();

		// Get order of bids
		List<Bid> bidOrder = br.getBidOrder();
		ArrayList<Bid> bids = new ArrayList<Bid>(bidOrder);
		double numbOfBids = bids.size();
		if (numbOfBids + 10 <= domain.getNumberOfPossibleBids() && user.getElicitationCost() < 0.01) {
			Random r = new Random();
			for (int q = 0; q < 10; q++) {
				Bid bidToAddInitially = domain.getRandomBid(r);
				if (bidOrder.contains(bidToAddInitially))
					continue;
				userModel = user.elicitRank(bidToAddInitially, userModel);
			}
		}

		// Count frequency of values in BidRanking
		HashMap<String, Integer> valueFreq = new HashMap<String, Integer>();
		// Get low and high utility
		double min = userModel.getBidRanking().getLowUtility();
		double max = userModel.getBidRanking().getHighUtility();
		if (numbOfBids <= 200) // regular number of bids
		{
			// Derivation of equation for exponential interpolation between min and max
			// Affiliate y = a f(x) + b where f(x) = e^x - 1 / e - 1
			// [https://math.stackexchange.com/questions/297768/how-would-i-create-a-exponential-ramp-function-from-0-0-to-1-1-with-a-single-val]
			// b will be our min in this case, so we need to derive a from 2 equations
			double a = (max - min) / ((Math.pow(Math.E, numbOfBids) - 1) / (Math.E - 1));
			for (int i = 0; i < numbOfBids; i++) {
				Bid current = bids.get(i);
				double currentUtility = (a * ((Math.pow(Math.E, i) - 1) / (Math.E - 1))) + min;
				List<Issue> issues = current.getIssues();
				for (Issue issue : issues) {
					int no = issue.getNumber();
					ValueDiscrete v = (ValueDiscrete) current.getValue(no);
					double oldUtil = factory.getUtility(issue, v);
					valueFreq.put(v.getValue(), valueFreq.getOrDefault(v.getValue(), 0) + 1);
					// [https://www.geeksforgeeks.org/properties-getordefaultkey-defaultvalue-method-in-java-with-examples/]
					factory.setUtility(issue, v, oldUtil + currentUtility);
				}
			}
		} else // large number of bids
		{
			// Linear interpolation is used for large number of bids instead of exponential
			// y - min = x * (max - min/ numbOfBids)
			// [https://study.com/academy/lesson/interpolation-in-statistics-definition-formula-example.html]
			for (int i = 0; i < numbOfBids; i++) {
				Bid current = bids.get(i);
				double currentUtility = (((max - min) / numbOfBids) * i) + min;
				List<Issue> issues = current.getIssues();
				for (Issue issue : issues) {
					int no = issue.getNumber();
					ValueDiscrete v = (ValueDiscrete) current.getValue(no);
					double oldUtil = factory.getUtility(issue, v);
					valueFreq.put(v.getValue(), valueFreq.getOrDefault(v.getValue(), 0) + 1);
					// [https://www.geeksforgeeks.org/properties-getordefaultkey-defaultvalue-method-in-java-with-examples/]
					factory.setUtility(issue, v, oldUtil + currentUtility);
				}
			}
		}
		// JohnnyBlack approach to estimate weights
		List<Issue> issues = factory.getDomain().getIssues();
		ArrayList<Double> issueWeight = new ArrayList<Double>();
		double forNormalization = 0.0;
		for (Issue issue : issues) {
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			ArrayList<Integer> valuesFreq = new ArrayList<Integer>();
			double sumOfValueWeights = 0.0;
			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				int specificValueFreq = valueFreq.get(valueDiscrete.getValue());
				valuesFreq.add(specificValueFreq);
			}
			for (int i = 0; i < valuesFreq.size(); i++) {
				sumOfValueWeights += (Math.pow(valuesFreq.get(i), 2) / Math.pow(numbOfBids, 2));
			}
			issueWeight.add(sumOfValueWeights);
		}
		for (int j = 0; j < issueWeight.size(); j++)
			forNormalization += issueWeight.get(j);
		int iterate = 0;
		for (Issue issue : issues) {
			factory.setWeight(issue, (issueWeight.get(iterate) / forNormalization));
			iterate++;
		}
		return factory.getUtilitySpace();
	}

	public double getEstimatedUtility(Bid b) {
		AdditiveUtilitySpace space = (AdditiveUtilitySpace) estimateUtilitySpace();
		List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
		if (bidOrder.contains(b)) {
			double util = space.getUtility(b);
			return util - 0.1;
		} else {
			double util = space.getUtility(b);
			if (util > 0.7) {
				userModel = user.elicitRank(b, userModel);
				space = (AdditiveUtilitySpace) estimateUtilitySpace();
				util = space.getUtility(b);
				return util - 0.1;
			} else {
				return util - 0.1;
			}
		}
	}
}
