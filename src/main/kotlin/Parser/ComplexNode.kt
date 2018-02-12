package Parser

class ComplexNode(type : NodeType) : NodeModel(type) {
    var childrens : Array<NodeModel> = arrayOf()
}