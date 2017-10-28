lock "3.9.0"

set :application, "shevek"
set :deploy_to, "/var/apps/#{fetch :application}"
set :repo_url, "ssh://dev@test.vitolen.com:14000/git/#{fetch :application}.git"
set :pty, true
append :linked_files, "dist/config.edn"

namespace :deploy do
  desc "Upload Shevek config file"
  task :upload do
    on roles(:all) do |host|
      upload! "cap/files/config.edn", "#{shared_path}/dist/config.edn"
    end
  end
  before 'deploy:check:linked_files', :upload

  desc "Generates the jar file"
  task :package do
    on roles(:all) do |host|
      execute "cd #{release_path}; boot package"
    end
  end
  after :updated, :package
  after :published, 'service:shevek:restart'
end
