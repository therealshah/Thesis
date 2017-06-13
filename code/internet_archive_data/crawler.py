from py_bing_search import PyBingWebSearch
from urlparse import urlparse,urljoin

'''
	-This a basic crawler that given a input query, gets the top 10 results from bing and returns them ( the url)
'''




pages = 0
badUrlExtentions = ['.jpg','.jpeg']
encounteredUrls = {}

# mMlCxUd5qmU5uDJ1w1VLbDkobVK905A9cZZhYkfqGHg=


# get the intial url list
# send a requent to bing using the words and get top ten urls
def getTopTen(query):
	# print 'length', len (encounteredUrls)
	# print 'pages = ',pages
	top_ten_urls = [] # hold the initial urls
	bing = PyBingWebSearch('Get your pyBing Key. Its free!',query,web_only=False)
	first_ten_results = bing.search(limit=20, format='json') #1-50
	#urlList, next_uri = bing.search(query, limit=10, format='json') # get the results
	counter = pages # count number of urls
	for result in first_ten_results:
		checkUrl(result.url,top_ten_urls)
		if (pages - counter >=10): # only care about top 10
			break
	return top_ten_urls


#lets download the pages
def checkUrl(url,top_ten_urls):
		global pages # count num of pages
		try:
			# Now will see which sites not to crawl
			parsed_uri = urlparse(url)
			base_url = '{uri.scheme}://{uri.netloc}/'.format(uri=parsed_uri) # get base domain
			if not base_url in encounteredUrls: # if we havent encountered it yet
				if validPage(base_url): # ck if its a valid pages
					encounteredUrls[base_url] = base_url
					#print 'Crawling ',base_url,pages
					top_ten_urls.append(base_url)
					pages+=1 # increment the num if pages
		except: 
			print 'exception'


def validPage(url):
	global badUrlExtentions
	for ext in badUrlExtentions: # crawl and ck if we encounter any of these extionsion, if we do, return false
		if url.find(ext) == 1:
			return False
	return True
