set :deploy_config_path, 'cap/deploy.rb'
set :stage_config_path, 'cap/stages'

# Load DSL and set up stages
require "capistrano/setup"

# Include default deployment tasks
require "capistrano/deploy"

# Load the SCM plugin appropriate to your project:
require "capistrano/scm/git"
install_plugin Capistrano::SCM::Git

# Include tasks from other gems included in your Gemfile
set :services, [:shevek]
require 'capistrano/service'
