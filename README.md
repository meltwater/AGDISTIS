# FHAI NLP AGDISTIS NED

> AGDISTIS - Agnostic Named Entity Disambiguation.

![BuildStatus](https://drone.meltwater.io/api/badges/meltwater/AGDISTIS/status.svg)


This repo is a fork of [https://github.com/dice-group/AGDISTIS](https://github.com/dice-group/AGDISTIS) with performance 
improvements, caching, corpus based neural disambiguation, and a bunch of other goodies.

Differently from AGDISTIS, this NED disambiguates against the Meltwater Graph (which includes DBPedia).

## How to build
Installing the service locally:

      mvn -U clean install package -s settings.xml							
      
Note that the 'clean' task will both delete the target folder.

## Repository Maintainers

This repository is maintained by [Team Karma](https://wiki.meltwater.net/pages/viewpage.action?pageId=74720584). 
You can contact us by sending a mail to [karma.fhai@meltwater.com](mailto:karma.fhai@meltwater.com) or by dropping a message in the **NLP (Karma)** Slack channel if you are a Meltwater engineer.

## How to Contribute

We gladly accept improvement suggestions, comments and pull requests. 
<!-- Before submitting a change please read the [contributing guidelines](https://wiki.meltwater.net/display/ENG/Horace+development+process+and+how+to+contribute).-->
