lock "3.9.0"

set :application, "shevek"
set :deploy_to, "/var/apps/#{fetch :application}"
set :repo_url, "ssh://trinity/git/#{fetch :application}.git"

namespace :deploy do
  desc "Generates the jar file"
  task :package do
    on roles(:all) do |host|
      execute "cd #{release_path}; boot package"
    end
  end
  after :published, :package
  after :package, 'service:shevek:restart'
end
