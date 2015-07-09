

Awestruct::Extensions::Pipeline.new do
  extension Awestruct::Extensions::Posts.new( '/news', :posts )
 # extension Awestruct::Extensions::Paginator.new( :posts, '/news/index', :per_page=>5 )
end
