Pushing to multiple environments:


1) This command tells git that you want to push from your local development 
branch to the master branch of your staging remote.
  It might look a little disorderly, but there’s a lot more going on - take a
look at the git book for a very in-depth exploration of refspecs.

git push staging development:master

For example:

git push kumbaya-node0 kumbaya-node0:master
