#!/usr/bin/env python
import subprocess
import collections
import uuid
import urllib
from random import randint
#randint(1,10)

# prefix declaration
p_schema = "http://schema.org/"
p_db = "http://www.xybermotive.com/supplier/"
p_rdfs = "http://www.w3.org/2000/01/rdf-schema#"
p_geo = "http://www.w3.org/2003/01/geo/wgs84_pos#"
p_suppl = "http://www.xybermotive.com/supplier/"
p_prod = "http://www.xybermotive.com/products/"
p_owl = "http://www.w3.org/2002/07/owl#"
p_dbpedia = "http://dbpedia.org/resource/"
p_xsd =  "http://www.w3.org/2001/XMLSchema#"
p_map = "http://www.xybermotive.com/supplier/#"
p_sc = "http://www.xybermotive.com/ontology/"
p_rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
p_xml = "http://www.w3.org/XML/1998/namespace"

supplQueue = collections.deque()
partsQueue = collections.deque()
prodCount = 0

supplCmd = "~/Development/apache-jena/bin/sparql --data=supplier_all.nt --query=queries/get_supplier.rq --results=csv | tail -n +2"
p = subprocess.Popen(supplCmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
for line in p.stdout.readlines():
    supplQueue.append(line.strip().replace('"',''))
retval = p.wait()


def getprodname(supplierUri):
    last = supplierUri.split('/')[-1]
    return urllib.quote(last+"_prod")

def getproduri(supplierUri):
    return p_prod + urllib.quote_plus(supplierUri) + "_prod"


def printprod(s, prodCount):
    print "<"+s+"> <"+p_schema+"manufacturer> <"+getproduri(s)+"> ."
    print "<"+getproduri(s)+"> <"+p_rdfs+"type> <"+p_schema+"Product> ."
    print "<"+getproduri(s)+"> <"+p_schema+"name> \""+getprodname(s)+"\" ."
    if prodCount == 1:
        print "<"+getproduri(s)+"> <"+p_rdfs+"comment> \"root product\" ."


def printparts(produri, numParts, supplQueue):
    for x in range(0, numParts-1):
        ss = supplQueue.popleft()
        quantity=randint(1,10)
        ppuri=p_prod+"part_"+uuid.uuid4().hex
        print "<"+produri+"> <"+p_sc+"productPart> <"+ppuri+"> ."
        print "<"+ppuri+"> <"+p_sc+"product> <"+getproduri(ss)+"> ."
        print "<"+ppuri+"> <"+p_sc+"quantity> \""+str(quantity)+"\"^^<"+p_xsd+"integer> ."
        partsQueue.append(ss)


partsQueue.append(supplQueue.popleft())
while 1 == 1:
    if len(partsQueue) > 0:
        s = partsQueue.popleft()
        prodCount = prodCount+1
        printprod(s, prodCount)
        numParts = min(randint(1,10), len(supplQueue))
        if numParts > 0:
            printparts(getproduri(s), numParts, supplQueue)
    else:
        break