package group20;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;

import java.util.*;
import java.util.Map.Entry;

public class DistributionOpponent {
	private final int WINDOW_SIZE = 7;
    private final double CHI_HYPOTHESIS = 0.05;
    private final double INCREASE_ALPHA = 0.1;
    private final double INCREASE_BETA = 5;

    private double time;

    private final double LAPLACE_EXPONENT = 0.5; //variable between 0 and 1
    private ArrayList<HashMap<ValueDiscrete,Integer>> issuesOptionFrequency;
    private ArrayList<Double> weightsOfIssues; //sum of weights is 1
    private ArrayList<HashMap<ValueDiscrete, Double>> valuesOfOptions; //Values are normalised to (0,1). They can be stored in the same hashmap cause why not?
    private ArrayList<Bid> bidHistory;
    private List<HashMap<ValueDiscrete, Double>> currentWindowOptionFrequency, previousWindowOptionFrequency;
    private ArrayList<Bid> currentWindowBids;
    private List<Issue> issues;

    public ArrayList<Double> getWeights(){
        return weightsOfIssues;
    }
    
    public ArrayList<HashMap<ValueDiscrete, Double>> getEvaluation(){
        return valuesOfOptions;
    }

    public Double getBidsUtility(Bid bid){
        Double utility = 0.0;
        int issueCounter = 0;
        for(Issue issue : bid.getIssues()){
            ValueDiscrete val = (ValueDiscrete) bid.getValue(issue);
            utility += weightsOfIssues.get(issueCounter) * valuesOfOptions.get(issueCounter).get(val);
            issueCounter++;
        }
        return utility;

    }

    //WHENEVER YOU USE ISSUE NUMBER, REMEMBER IT'S OFF BY ONE
    public DistributionOpponent(List<Issue> issues){
        this.issues = issues;
        issuesOptionFrequency = new ArrayList<HashMap<ValueDiscrete, Integer>>();
        weightsOfIssues = new ArrayList<>();
        bidHistory = new ArrayList<>();
        valuesOfOptions = new  ArrayList<HashMap<ValueDiscrete, Double>>();
        currentWindowBids = new ArrayList<Bid>();

        for(Issue issue : issues){

            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;

            issuesOptionFrequency.add(new HashMap<ValueDiscrete, Integer>());
            valuesOfOptions.add(new HashMap<ValueDiscrete, Double>());
            weightsOfIssues.add(new Double(1.0/issues.size()));

            //Start putting Pairs into the issue array
            Integer counter = 1;
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()){

                //Initialize each value in both hashmaps to 0: occurences and value
                issuesOptionFrequency.get(issueNumber-1).put(valueDiscrete, 0);
                valuesOfOptions.get(issueNumber-1).put(valueDiscrete, 0.0);
            }

        }
    }

    private List<HashMap<ValueDiscrete, Double>>  getNewFrequencyDistributionMap(List<Issue> issues){
        List<HashMap<ValueDiscrete, Double>> newDistribution = new ArrayList<HashMap<ValueDiscrete, Double>>();


        for(Issue issue : issues){
            newDistribution.add(new HashMap<ValueDiscrete, Double>());
            int issueNumber = issue.getNumber();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issue;

            //Start putting Pairs into the issue array
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()){
                //Initialize each value in both hashmaps to 0: occurences and value
                newDistribution.get(issueNumber-1).put(valueDiscrete, 0.0);
            }
        }
        return newDistribution;
    }

    public void receiveBid(Bid bid, double time){
        System.out.println("Bid util: " + getBidsUtility(bid));
        this.time = time;
        updateFrequencyCounter(bid, issuesOptionFrequency);
        updateValues(bid);
        currentWindowBids.add(bid);
        if(currentWindowBids.size()>=WINDOW_SIZE){
            updateIssueWeigths();
            currentWindowBids = new ArrayList<Bid>();
            //printValuesForEachIssue();
            System.out.println("Dist: " + weightsOfIssues);

        }

    }

    private void updateValues(Bid bid){

        //for each issue
        for(int issueIndex = 0 ; issueIndex < issuesOptionFrequency.size(); issueIndex++){
            HashMap<ValueDiscrete,Double> issueValueMap = valuesOfOptions.get(issueIndex);
            HashMap<ValueDiscrete,Integer> issueFrequencyMap = issuesOptionFrequency.get(issueIndex);

            //find maximal frequency count fro the given issue
            Double maxCount = 1.0 +  Collections.max(issueFrequencyMap.values()).doubleValue();

            //update all values
            for(ValueDiscrete valueDiscrete: issueValueMap.keySet()){
                Double valueCount = 1.0 +  issueFrequencyMap.get(valueDiscrete).doubleValue();
                //Laplace smoothing
                Double updatedValue = Math.pow(valueCount / maxCount, LAPLACE_EXPONENT);
                issueValueMap.put(valueDiscrete, updatedValue);
            }

        }
        return;
    }



    /*Updates frequency maps in a given distribution*/
    public ArrayList updateFrequencyCounter(Bid lastOffer, ArrayList<HashMap<ValueDiscrete,Integer>> distribution) {
        for (Issue issue : lastOffer.getIssues()) {
            HashMap<ValueDiscrete, Integer> map = distribution.get(issue.getNumber() - 1);
            ValueDiscrete value = (ValueDiscrete) lastOffer.getValue(issue);
            map.put(value, map.get(value) + 1);
        }

        return distribution;
    }


    /*Weights updates:
    * -effect decays over time d(t) = alfa * (1-t^beta)
    *
    *Arguments takes the two latest consecutive and disjont windows of offers
    *
    * data:
    * t - the current time of negotiation
    * O' - the previous parition(window?) of k offers
    * O - the current partition of K offers
    * O1->t - all offers received so far
    * W' = {w'1, ... w'n} the current weights for opponent model
    *
    * Result:
    * W = {w1, w2, ... wn} - new weigths for opponent model*/

    private void initialiseWeightsOfIssues(){
        for(int i = 0; i < weightsOfIssues.size(); i++){
            weightsOfIssues.set(i, 1.0/weightsOfIssues.size());
        }
    }
    private ArrayList<Double> copyWeightList(ArrayList<Double> weights){
        ArrayList<Double> newWeigths = new ArrayList<Double>();
        for(Double wgh : weights){
            newWeigths.add(wgh); // should work. double is immutable so it should create a new one with the same value
        }
        return newWeigths;
    }

    private void updateIssueWeigths() {
        //Initially, new weights are the copy of old weigths
        ArrayList<Double> newWeigths = copyWeightList(weightsOfIssues);
        //System.out.println("New weigths DUPA:");
        //System.out.println(newWeigths);

        /*Now, we want co calculate frequency distributions for new window*/
        previousWindowOptionFrequency = currentWindowOptionFrequency;
        currentWindowOptionFrequency = buildWindowFrequencyDistribution(currentWindowBids);

        //There is no old frequency distriubtion: return
        if(previousWindowOptionFrequency == null){
            return;
        }

        //Now, for every issue check if the distriubtions are statistically equivalent

        for(int issueIndex = 0; issueIndex < issuesOptionFrequency.size(); issueIndex++){
            boolean distributionsDifferent = !(chiSquaredTest(previousWindowOptionFrequency.get(issueIndex), currentWindowOptionFrequency.get(issueIndex)));

            /*
            Forget this for now. f the distributions are the same, we will just increase the weigths
            if(distributionsDifferent){
                //Use previously calculated evaluations for options to calulcate utilities for old and new distriubtion
                Double oldUtil = 0.0;
                Double newUtil = 0.0;
                HashMap<ValueDiscrete, Double> mapOfOptionsValues = valuesOfOptions.get(issueIndex);
                for(ValueDiscrete key: previousWindowOptionFrequency.get(issueIndex).keySet()){
                    oldUtil += mapOfOptionsValues.get(key) * previousWindowOptionFrequency.get(issueIndex).get(key);
                    newUtil += mapOfOptionsValues.get(key) * currentWindowOptionFrequency.get(issueIndex).get(key);
                }
                if(newUtil < oldUtil){
                    //Concession -
                }
            }
             */


            if(!distributionsDifferent){
                //increase weights
                newWeigths.set(issueIndex,  newWeigths.get(issueIndex) + INCREASE_ALPHA * (1 - Math.pow(time, INCREASE_BETA)));

            }
           // System.out.println("AFTER CHANGE");
           // System.out.println(newWeigths);

        }

        normaliseWeights(newWeigths);
        weightsOfIssues = newWeigths;
    }

    private void normaliseWeights(ArrayList<Double> weights){
        Double coef = weights.stream().mapToDouble(a -> a).sum();
        for(int i = 0; i < weights.size(); i++){
            weights.set(i, weights.get(i)/coef);
        }


    }

    //Check if distributions are the same
    private boolean chiSquaredTest(HashMap<ValueDiscrete, Double> oldDistr, HashMap<ValueDiscrete, Double> newDistr){
        //http://www.stat.yale.edu/Courses/1997-98/101/chigf.htm

        Double chiSquared = 0.0;
        //System.out.println("CHI BEFORE: " + chiSquared);
       // System.out.println(oldDistr);

        for(ValueDiscrete key: oldDistr.keySet()){
            //Old is expected
            chiSquared += Math.pow(oldDistr.get(key)-newDistr.get(key), 2)/oldDistr.get(key);

        }
        //System.out.println("Chi squared after: " + chiSquared);
        return chiSquared < CHI_HYPOTHESIS;
    }



    //Frequency of windows
    private List<HashMap<ValueDiscrete, Double>> buildWindowFrequencyDistribution(ArrayList<Bid> currentWindowBids){
        List<HashMap<ValueDiscrete, Double>> distribution = getNewFrequencyDistributionMap(issues);
        //Add each value from each bid to the hasmap
        for(Bid bid : currentWindowBids){
            for(Issue issue : bid.getIssues()){
                HashMap<ValueDiscrete, Double> map = distribution.get(issue.getNumber() - 1);
                ValueDiscrete value = (ValueDiscrete) bid.getValue(issue);
                map.put(value, map.get(value) + 1);
            }
        }

        //Normalization: divide every value in the distribution by the number of bids in the window
        Double count = ((Integer)currentWindowBids.size()).doubleValue();
        //System.out.println("COUNT IS " + count);

        for(int i = 0; i < distribution.size(); i++){
            HashMap<ValueDiscrete, Double> map = distribution.get(i);
           // System.out.println("DISTR BEFORE NORM: " + map);

            for(ValueDiscrete key : map.keySet()){
                map.put(key, (map.get(key) + 1.0 )/ (count + 1.0));
            }
           // System.out.println("DISTR AFTER NORM: " + map);

        }

        return distribution;

    }



    public void printValuesForEachIssue(){
        for(HashMap map : valuesOfOptions){
            System.out.println("Another issue: " + Arrays.asList(map));
        }
    }

}
