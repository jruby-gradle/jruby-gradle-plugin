

Awestruct::Extensions::Pipeline.new do
  extension Awestruct::Extensions::Posts.new( '/news', :posts )
  #extension Awestruct::Extensions::Indexifier.new
  extension Awestruct::Extensions::Sitemap.new
  extension Awestruct::Extensions::Atomizer.new(:posts, '/news.atom', :feed_title => 'JRuby/Gradle News')
end
