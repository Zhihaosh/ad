import sys
import json
from pyspark import SparkContext

def get_term(line):
    entry = line.split('_')
    return entry[1]

def get_adid_terms(line):
    entry = json.loads(line.strip())
    ad_id = entry['adId']
    adid_terms = []
    #print entry['keyWords']
    for term in entry['keyWords']:
        val = str(ad_id) + "_" + term
        adid_terms.append(val)
    return adid_terms

def generate_json(items):
    result = {}
    result['doc_freq'] = items[0]
    result['count'] = items[1]
    return json.dumps(result)

if __name__ == "__main__":
    adfile = sys.argv[1] #raw search log
    sc = SparkContext(appName="DF_Features")
    data = sc.textFile(adfile).flatMap(lambda line: get_adid_terms(line)).
    distinct().map(lambda w: (get_term(w),1)).reduceByKey(lambda v1,v2: v1 + v2).
    map(generate_json)
    data.saveAsTextFile("/Users/jiayangan/project/SearchAds/data/log/DF")
    sc.stop()
