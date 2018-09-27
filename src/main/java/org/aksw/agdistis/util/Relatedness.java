package org.aksw.agdistis.util;

import java.util.List;

import org.aksw.agdistis.datatypes.AnchorDocument;

public class Relatedness {

    public static void main(String[] args) {

    }
    
    final double KG_SIZE = 3682042;
    public static final int INLINKS_LIMIT = 500;
    
    public double getRelatedness(AnchorDocument A, AnchorDocument B){
            
        if(A == null || B == null)return 0d;
        return relatedNessFunction(A.inLinks.size(), B.inLinks.size(), A.inLinks, B.inLinks);
     }
    
    
    
    
    private double relatedNessFunction(int countA, int countB, List<Integer> A, List<Integer> B) {
        int i = 0;
        int j = 0;
        int countCommon = 0;
        if ((INLINKS_LIMIT == -1) || ((countA <= INLINKS_LIMIT) && (countB <= INLINKS_LIMIT))) {
          /**
           * search in full id ranges
           */
          while ((i < countA) && (j < countB)) {
            final int diff = A.get(i) - B.get(j);
            if (diff < 0) {
              i++;
            } else if (diff > 0) {
              j++;
            } else {
              countCommon++;
              i++;
              j++;
            }
          }
          return distance(countA, countB, countCommon);
        } else {
          /**
           * subsampled matching and extrapolating result
           */
          int countA2 = countA;
          int countB2 = countB;
          double ratioA = 1;
          if (countA > INLINKS_LIMIT) {
            ratioA = (double) countA / INLINKS_LIMIT;
            countA2 = INLINKS_LIMIT;
          }
          double ratioB = 1;
          if (countB > INLINKS_LIMIT) {
            ratioB = (double) countB / INLINKS_LIMIT;
            countB2 = INLINKS_LIMIT;
          }
          double iD = i;
          double jD = j;
          while ((i < countA) && (j < countB)) {
            final int diff = A.get(i) - B.get(j);
            if (diff < 0) {
              iD += ratioA;
              i = (int) iD;
            } else if (diff > 0) {
              jD += ratioB;
              j = (int) jD;
            } else {
              countCommon++;
              iD += ratioA;
              i = (int) iD;
              jD += ratioB;
              j = (int) jD;
            }
          }
          return distance(countA2, countB2, countCommon);
        }
      }
    
    
    private double distance(double countA, double countB, double countCommon) {
        if (countCommon == 0) {
          return 0;
        }
        
//        double score = 2.0d * ((double)countCommon/((double)countA + (double)countB));
//        return score;
       
        final double maxCount = Math.max(countA, countB);
        final double minCount = Math.min(countA, countB);
//        normalized google distance (logW: log(kgsize))
        final double kgsize = Math.log(KG_SIZE);
        return 1.0 - (Math.log(maxCount) - Math.log(countCommon)) / (kgsize - Math.log(minCount));
        
      }
    
    public class Key {

        private final int x;
        private final int y;

        public Key(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return x == key.x && y == key.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

    }

}
