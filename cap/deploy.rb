lock "3.9.0"

set :application, "shevek"
set :deploy_to, "/var/apps/#{fetch :application}"
set :repo_url, "ssh://dev@test.vitolen.com:14000//git/#{fetch :application}.git"
set :pty, true

namespace :deploy do
  desc "Generates the jar file"
  task :package do
    on roles(:all) do |host|
      execute "cd #{release_path}; boot package"
    end
  end
  after :updated, :package
  after :published, 'service:shevek:restart'
end
