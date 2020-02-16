Project.configure do |project|
  project.email_notifier.emails = %w(emmanicolau@gmail.com)
  project.build_command = 'npm i && DISPLAY=:1 lein ci'
end
