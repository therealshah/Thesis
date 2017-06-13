In this section, we present the code which was used to setup the Internet Archive dataset. The data was generated in the following way:



Description of the Program:

 We selected three random words from the English dictionary. The three random words were used as a search query in Bing and the top 20 results were selected. Only the top 10 unique results were kept and this process was repeated until we obtained 5000 unique URLS.
Then, we used the Wayback API (https://archive.org/help/wayback_api.php)  to retrieve four versions of the page: current, six months ago, a year ago, two years ago. We then filtered the datasets and removed any pages that did not have 4 distinct uncorrupted versions, and were left with 4427 unique pages. The chunk signatures of each of the previous versions were compared to the chunk signatures of the current version. 



How to run the program:

	1. Run getUrls.py - retrieves 5000 (can easily change) unique urls by randomly selecting a words and quering bing. 
	2. Run wayback.py - For each URL, using wayback api to retrieve 4 different versions. 
	3. Run countFiles.py - Sums up all the unique documents we are left with. Also removes all the pages that do not have 4 unique versions (may have to uncomment the code)