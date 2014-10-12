#!/usr/bin/env ruby

require 'json'
require 'net/https'
require 'uri'

def uri_for(project, milestone)
  return URI("https://api.github.com/repos/jruby-gradle/#{project}/issues?state=closed&milestone=#{milestone}")
end

def changelog_for(project, milestone)
  uri = uri_for(project, milestone)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  response = http.get(uri.request_uri)

  raise "Status #{response.code}" unless response.code.to_i == 200

  body = JSON.parse(response.body)

  body.reverse.each do |issue|
    puts "* [##{issue['number']}](#{issue['html_url']}) - #{issue['title']}"
  end
end

print 'What project? > '
project = STDIN.gets.chomp

print 'What milestone? > '
milestone = STDIN.gets.chomp

puts
puts "Computing changelog for '#{project}' and milestone '#{milestone}'"
puts
puts '-------------------'

changelog_for(project, milestone)
