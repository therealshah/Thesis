import random
import crawler

'''

	This file gets the dataset for experimentation for my thesis
	Basically picks 3 words as random from the english dictioanry ( file was found in my local hardrive with around 96k words)
	seraches those queries on bing and get otp 10 results if applicable and writes the urls to a file
	We repeat the process until we have 5000 urls

'''


word_list = [] # stores the word list
url_list = [] # used to store all the urls
file_name = 'american-english'

limit = 5000

# Skips these many numbers
def skip(skips):
	for i in range(skips):
		random.randint(1,len(word_list)-2) # skipping these numbers

# Open the dictionary file and read it
def read_file():
	#open file for reading
	with open(file_name,'r') as file:
		# loop through each line
		for line in file:
			word_list.append(line.strip('\n')) # add the word to the l

# Randomly picks 3 letters and gets top 10 result from bing 
# We repeat this process until we have about 3000 urls
def get_urls():
	global url_list

	seed = input("Enter the seed ") #
	skips = input ("Enter number of skips ") #number of skips
	random.seed(seed)

	while len(url_list) < limit:
		print 'We have ',len(url_list),' Urls'
		skip(seed) # used to increase randomness
		index1 = random.randint(0,len(word_list)-2) # subtract 2 because last index is an empty space which we dont want
		skip(seed)
		index2 = random.randint(0,len(word_list)-2)
		skip(seed)
		index3 = random.randint(0,len(word_list)-2)
		query = word_list[index1] + " " + word_list[index2] + " " + word_list[index3] # this is the search query 
		# print query
		top_ten_results = crawler.getTopTen(query) # call crawler to get top ten results for this query
		url_list = url_list + top_ten_results # concatenate the results
		#print (top_ten_results)



# Basically writes the urls we got to a local file, that way we dont have to reget all the links
def write_url_to_file():
	with open('Internet_Archive_DataSet.txt','w') as file:
		for url in url_list:
			file.write(url + '\n')


read_file()
get_urls()
write_url_to_file()